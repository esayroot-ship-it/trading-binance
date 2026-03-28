package org.example.marketsvc.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.entity.AssetInfo;
import org.example.marketsvc.config.BinanceMarketProperties;
import org.example.marketsvc.constant.MarketRedisKeys;
import org.example.marketsvc.dto.BinanceTickerPriceResponse;
import org.example.marketsvc.dto.MarketKlineResponse;
import org.example.marketsvc.dto.MarketQuoteResponse;
import org.example.marketsvc.dto.MarketWsKlineSnapshotResponse;
import org.example.marketsvc.mapper.AssetInfoMapper;
import org.example.marketsvc.mq.MarketPriceChangeProducer;
import org.example.marketsvc.service.MarketDataService;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataServiceImpl implements MarketDataService {

    private static final String ASSET_TYPE_CRYPTO = "CRYPTO";
    private static final String PRICE_API = "/api/v3/ticker/price?symbol={symbol}";
    private static final String KLINE_API = "/api/v3/klines?symbol={symbol}&interval={interval}&limit={limit}";
    private static final String WS_COMBINED_STREAM_PATH = "/stream?streams=";
    private static final int WS_CLOSE_TIMEOUT_SECONDS = 2;
    private static final int CHANGE_SCALE = 8;

    private final AssetInfoMapper assetInfoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final BinanceMarketProperties binanceMarketProperties;
    private final MarketPriceChangeProducer marketPriceChangeProducer;
    private final Object wsMonitor = new Object();
    private final Object klineWriteMonitor = new Object();
    private final AtomicLong wsSessionId = new AtomicLong(0);
    private final HttpClient wsHttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor(
            new NamedThreadFactory("market-ws-reconnect"));

    private volatile WebSocket wsClient;
    private volatile List<String> subscribedSymbols = List.of();
    private volatile List<String> subscribedTimeframes = List.of();
    private volatile boolean shuttingDown = false;

    @Override
    public void warmupMarketCache() {
        reloadMarketCacheAndStream();
    }

    @Override
    public void refreshMarketCache() {
        ensureRealtimeStreamConnected();
    }

    @Override
    public void refreshKlineCacheByRest() {
        List<String> symbols = loadActiveSymbols();
        List<String> timeframes = resolveTrackedTimeframes();
        if (symbols.isEmpty()) {
            log.warn("定时刷新K线已跳过, 当前无可用资产");
            return;
        }
        log.info("开始按REST定时刷新K线缓存, 标的数量={}, 周期数量={}", symbols.size(), timeframes.size());
        for (String symbol : symbols) {
            for (String timeframe : timeframes) {
                String klineJson = fetchKlineJson(symbol, timeframe, safeHistoryLimit());
                if (!StringUtils.hasText(klineJson)) {
                    continue;
                }
                cacheKline(symbol, timeframe, klineJson);
            }
        }
        log.info("REST定时刷新K线缓存完成, 标的数量={}, 周期数量={}", symbols.size(), timeframes.size());
    }

    @Override
    public void reloadMarketCacheAndStream() {
        List<String> symbols = loadActiveSymbols();
        List<String> timeframes = resolveTrackedTimeframes();
        log.info("开始重载市场缓存与实时流, 资产数量={}, K线周期={}", symbols.size(), timeframes);
        if (symbols.isEmpty()) {
            log.warn("当前没有可用资产, 资产类型为CRYPTO且状态为1, 不初始化行情缓存");
        }
        clearMarketCache();
        for (String symbol : symbols) {
            initializeSymbolCache(symbol, timeframes);
        }
        subscribedSymbols = symbols;
        subscribedTimeframes = timeframes;
        reconnectRealtimeStream(symbols, timeframes);
        log.info("市场缓存重载完成, 标的数量={}, 周期数量={}", symbols.size(), timeframes.size());
    }

    @Override
    public List<AssetInfo> listActiveAssets() {
        return assetInfoMapper.selectByAssetTypeAndStatus(ASSET_TYPE_CRYPTO, 1);
    }

    @Override
    public MarketQuoteResponse getQuote(String symbol) {
        String safeSymbol = normalizeSymbol(symbol);
        String cacheKey = MarketRedisKeys.priceKey(safeSymbol);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            try {
                return objectMapper.readValue(cached, MarketQuoteResponse.class);
            } catch (Exception ex) {
                log.warn("解析行情缓存失败, 标的={}", safeSymbol, ex);
            }
        }

        BigDecimal latestPrice = fetchLatestPrice(safeSymbol);
        if (latestPrice == null) {
            return null;
        }
        MarketQuoteResponse response = buildQuote(safeSymbol, latestPrice);
        cacheQuote(response);
        return response;
    }

    @Override
    public MarketKlineResponse getKline(String symbol, String timeframe) {
        String safeSymbol = normalizeSymbol(symbol);
        String safeTimeframe = normalizeTimeframe(timeframe);
        String cached = loadKlineJson(safeSymbol, safeTimeframe);
        if (!StringUtils.hasText(cached)) {
            return null;
        }

        MarketKlineResponse response = new MarketKlineResponse();
        response.setSymbol(safeSymbol);
        response.setTimeframe(safeTimeframe);
        response.setKlineJson(cached);
        response.setLastKlineTime(extractLastKlineTime(cached));
        return response;
    }

    @Override
    public MarketWsKlineSnapshotResponse getFrontendPriceSnapshot(String symbol, String timeframe) {
        String safeSymbol = normalizeSymbol(symbol);
        String safeTimeframe = normalizeTimeframe(timeframe);
        String klineJson = loadKlineJson(safeSymbol, safeTimeframe);
        if (!StringUtils.hasText(klineJson)) {
            return null;
        }

        List<CandleSnapshot> candles = parseCandles(klineJson);
        if (candles.isEmpty()) {
            return null;
        }

        CandleSnapshot latest = candles.get(candles.size() - 1);
        CandleSnapshot previous = candles.size() > 1 ? candles.get(candles.size() - 2) : null;

        BigDecimal changeAmount = BigDecimal.ZERO;
        BigDecimal changePercent = BigDecimal.ZERO;
        if (previous != null && previous.close != null && previous.close.compareTo(BigDecimal.ZERO) != 0) {
            changeAmount = latest.close.subtract(previous.close);
            changePercent = changeAmount
                    .divide(previous.close, CHANGE_SCALE, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        MarketQuoteResponse quote = getQuote(safeSymbol);
        BigDecimal currentPrice = quote != null && quote.getPrice() != null ? quote.getPrice() : latest.close;
        long updateTime = quote != null && quote.getUpdateTime() != null
                ? quote.getUpdateTime()
                : (latest.closeTime > 0 ? latest.closeTime : System.currentTimeMillis());

        MarketWsKlineSnapshotResponse response = new MarketWsKlineSnapshotResponse();
        response.setStream(safeSymbol.toLowerCase(Locale.ROOT) + "@kline_" + safeTimeframe.toLowerCase(Locale.ROOT));
        response.setData(buildWsKlineData(safeSymbol, safeTimeframe, latest, updateTime));
        response.setKlineJson(klineJson);
        response.setCurrentPrice(currentPrice);
        response.setTwoKlineChangeAmount(changeAmount);
        response.setTwoKlineChangePercent(changePercent);
        response.setRising(changeAmount.compareTo(BigDecimal.ZERO) >= 0);
        response.setUpdateTime(updateTime);
        return response;
    }

    @PreDestroy
    public void shutdown() {
        shuttingDown = true;
        wsSessionId.incrementAndGet();
        closeWebSocketSilently();
        reconnectExecutor.shutdownNow();
    }

    private void ensureRealtimeStreamConnected() {
        synchronized (wsMonitor) {
            List<String> symbolSnapshot = subscribedSymbols;
            if (symbolSnapshot.isEmpty()) {
                symbolSnapshot = loadActiveSymbols();
                subscribedSymbols = symbolSnapshot;
            }
            List<String> timeframeSnapshot = subscribedTimeframes;
            if (timeframeSnapshot.isEmpty()) {
                timeframeSnapshot = resolveTrackedTimeframes();
                subscribedTimeframes = timeframeSnapshot;
            }
            if (symbolSnapshot.isEmpty()) {
                closeWebSocketSilently();
                return;
            }
            WebSocket current = wsClient;
            if (current == null || current.isInputClosed() || current.isOutputClosed()) {
                reconnectRealtimeStream(symbolSnapshot, timeframeSnapshot);
            }
        }
    }

    private void initializeSymbolCache(String symbol, List<String> timeframes) {
        if (!StringUtils.hasText(symbol)) {
            return;
        }
        String safeSymbol = symbol.trim().toUpperCase(Locale.ROOT);
        log.info("开始初始化资产缓存, 标的={}, 周期数量={}", safeSymbol, timeframes.size());

        BigDecimal latestPrice = fetchLatestPrice(safeSymbol);
        if (latestPrice != null) {
            cacheQuote(buildQuote(safeSymbol, latestPrice));
            log.info("初始化价格缓存成功, 标的={}, 价格={}", safeSymbol, latestPrice);
        } else {
            log.warn("初始化价格缓存失败, 标的={}, 未获取到最新价格", safeSymbol);
        }

        for (String timeframe : timeframes) {
            String klineJson = fetchKlineJson(safeSymbol, timeframe, safeHistoryLimit());
            cacheKline(safeSymbol, timeframe, klineJson);
            if (StringUtils.hasText(klineJson)) {
                log.info("初始化K线缓存成功, 标的={}, 周期={}, 字节数={}",
                        safeSymbol, timeframe, klineJson.length());
            } else {
                log.warn("初始化K线缓存失败, 标的={}, 周期={}, 返回为空", safeSymbol, timeframe);
            }
        }
    }

    private String loadKlineJson(String safeSymbol, String safeTimeframe) {
        String cacheKey = MarketRedisKeys.klineKey(safeSymbol, safeTimeframe);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            return cached;
        }
        String fetched = fetchKlineJson(safeSymbol, safeTimeframe, safeHistoryLimit());
        if (!StringUtils.hasText(fetched)) {
            return null;
        }
        cacheKline(safeSymbol, safeTimeframe, fetched);
        return fetched;
    }

    private List<CandleSnapshot> parseCandles(String klineJson) {
        List<CandleSnapshot> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(klineJson);
            if (!root.isArray()) {
                return result;
            }
            for (JsonNode candleNode : root) {
                CandleSnapshot candle = parseCandle(candleNode);
                if (candle != null && candle.close != null) {
                    result.add(candle);
                }
            }
        } catch (Exception ex) {
            log.warn("解析K线数据失败", ex);
        }
        return result;
    }

    private CandleSnapshot parseCandle(JsonNode candleNode) {
        if (candleNode == null || !candleNode.isArray() || candleNode.size() < 6) {
            return null;
        }
        CandleSnapshot snapshot = new CandleSnapshot();
        snapshot.openTime = candleNode.get(0).asLong();
        snapshot.open = candleNode.get(1).asText("0");
        snapshot.high = candleNode.get(2).asText("0");
        snapshot.low = candleNode.get(3).asText("0");
        snapshot.close = parseBigDecimal(candleNode.get(4).asText("0"));
        snapshot.closeRaw = candleNode.get(4).asText("0");
        snapshot.volume = candleNode.get(5).asText("0");
        snapshot.closeTime = candleNode.size() > 6 ? candleNode.get(6).asLong() : snapshot.openTime;
        snapshot.quoteVolume = candleNode.size() > 7 ? candleNode.get(7).asText("0") : "0";
        snapshot.tradeCount = candleNode.size() > 8 ? candleNode.get(8).asInt(0) : 0;
        snapshot.takerBuyBaseVolume = candleNode.size() > 9 ? candleNode.get(9).asText("0") : "0";
        snapshot.takerBuyQuoteVolume = candleNode.size() > 10 ? candleNode.get(10).asText("0") : "0";
        snapshot.ignore = candleNode.size() > 11 ? candleNode.get(11).asText("0") : "0";
        return snapshot;
    }

    private BigDecimal parseBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private MarketWsKlineSnapshotResponse.WsKlineData buildWsKlineData(
            String symbol,
            String timeframe,
            CandleSnapshot latest,
            long updateTime) {
        MarketWsKlineSnapshotResponse.WsKlineData data = new MarketWsKlineSnapshotResponse.WsKlineData();
        data.setE("kline");
        data.setEventTime(updateTime);
        data.setS(symbol);

        MarketWsKlineSnapshotResponse.WsKlinePayload payload = new MarketWsKlineSnapshotResponse.WsKlinePayload();
        payload.setOpenTime(latest.openTime);
        payload.setCloseTime(latest.closeTime);
        payload.setSymbol(symbol);
        payload.setInterval(timeframe);
        payload.setOpen(latest.open);
        payload.setClose(latest.closeRaw);
        payload.setHigh(latest.high);
        payload.setLow(latest.low);
        payload.setVolume(latest.volume);
        payload.setTradeCount(latest.tradeCount);
        payload.setClosed(Boolean.TRUE);
        payload.setQuoteVolume(latest.quoteVolume);
        payload.setTakerBuyBaseVolume(latest.takerBuyBaseVolume);
        payload.setTakerBuyQuoteVolume(latest.takerBuyQuoteVolume);
        payload.setIgnore(latest.ignore);
        data.setK(payload);
        return data;
    }

    private BigDecimal fetchLatestPrice(String symbol) {
        String url = binanceMarketProperties.getBaseUrl() + PRICE_API;
        log.info("开始抓取最新价格, 标的={}, 地址={}", symbol, url);
        try {
            BinanceTickerPriceResponse response = restTemplate.getForObject(
                    url,
                    BinanceTickerPriceResponse.class,
                    symbol);
            if (response == null || !StringUtils.hasText(response.getPrice())) {
                log.warn("抓取最新价格结果为空, 标的={}", symbol);
                return null;
            }
            log.info("抓取最新价格成功, 标的={}, 价格={}", symbol, response.getPrice());
            return new BigDecimal(response.getPrice());
        } catch (Exception ex) {
            log.warn("抓取最新价格失败, 标的={}, 原因={}", symbol, ex.getMessage(), ex);
            return null;
        }
    }

    private String fetchKlineJson(String symbol, String timeframe, int limit) {
        String url = binanceMarketProperties.getBaseUrl() + KLINE_API;
        log.info("开始抓取K线, 标的={}, 周期={}, 条数上限={}, 地址={}", symbol, timeframe, limit, url);
        try {
            String response = restTemplate.getForObject(
                    url,
                    String.class,
                    symbol,
                    timeframe,
                    limit);
            if (!StringUtils.hasText(response)) {
                log.warn("抓取K线结果为空, 标的={}, 周期={}, 条数上限={}", symbol, timeframe, limit);
            } else {
                log.info("抓取K线成功, 标的={}, 周期={}, 字节数={}", symbol, timeframe, response.length());
            }
            return response;
        } catch (Exception ex) {
            log.warn("抓取K线失败, 标的={}, 周期={}, 条数上限={}, 原因={}",
                    symbol, timeframe, limit, ex.getMessage(), ex);
            return null;
        }
    }

    private void cacheKline(String symbol, String timeframe, String klineJson) {
        if (!StringUtils.hasText(klineJson)) {
            return;
        }
        stringRedisTemplate.opsForValue().set(MarketRedisKeys.klineKey(symbol, timeframe), klineJson);
    }

    private String mergeKlineJson(String existingJson, String latestJson, int maxSize) {
        if (!StringUtils.hasText(existingJson)) {
            return latestJson;
        }
        try {
            JsonNode existingNode = objectMapper.readTree(existingJson);
            JsonNode latestNode = objectMapper.readTree(latestJson);
            if (!(existingNode instanceof ArrayNode existingArray) || !(latestNode instanceof ArrayNode latestArray)) {
                return latestJson;
            }

            Map<Long, JsonNode> merged = new LinkedHashMap<>();
            for (JsonNode node : existingArray) {
                long openTime = parseOpenTime(node);
                if (openTime > 0) {
                    merged.put(openTime, node.deepCopy());
                }
            }
            for (JsonNode node : latestArray) {
                long openTime = parseOpenTime(node);
                if (openTime > 0) {
                    merged.put(openTime, node.deepCopy());
                }
            }

            while (merged.size() > maxSize) {
                Long firstKey = merged.keySet().iterator().next();
                merged.remove(firstKey);
            }

            ArrayNode result = objectMapper.createArrayNode();
            for (JsonNode node : merged.values()) {
                result.add(node);
            }
            return objectMapper.writeValueAsString(result);
        } catch (Exception ex) {
            log.warn("合并K线结构化文本失败", ex);
            return latestJson;
        }
    }

    private void reconnectRealtimeStream(List<String> symbols, List<String> timeframes) {
        synchronized (wsMonitor) {
            long session = wsSessionId.incrementAndGet();
            closeWebSocketSilently();
            if (shuttingDown || symbols.isEmpty()) {
                return;
            }
            List<String> safeTimeframes = (timeframes == null || timeframes.isEmpty())
                    ? resolveTrackedTimeframes()
                    : timeframes;

            String wsUrl = buildWsUrl(symbols, safeTimeframes);
            try {
                wsClient = wsHttpClient.newWebSocketBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .buildAsync(URI.create(wsUrl), new BinanceStreamListener(session))
                        .join();
                log.info("Binance长连接已连接, 会话={}, 标的数量={}, 周期数量={}",
                        session, symbols.size(), safeTimeframes.size());
            } catch (Exception ex) {
                log.error("连接Binance长连接失败, 会话={}", session, ex);
                scheduleReconnect(session);
            }
        }
    }

    private void scheduleReconnect(long session) {
        if (shuttingDown) {
            return;
        }
        long reconnectDelayMs = safeReconnectDelayMs();
        reconnectExecutor.schedule(() -> {
            if (shuttingDown || wsSessionId.get() != session) {
                return;
            }
            reconnectRealtimeStream(subscribedSymbols, subscribedTimeframes);
        }, reconnectDelayMs, TimeUnit.MILLISECONDS);
    }

    private void closeWebSocketSilently() {
        WebSocket current = wsClient;
        wsClient = null;
        if (current == null) {
            return;
        }
        try {
            current.sendClose(WebSocket.NORMAL_CLOSURE, "reconnect")
                    .orTimeout(WS_CLOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .join();
        } catch (Exception ex) {
            log.debug("关闭长连接时忽略异常", ex);
        }
    }

    private void handleWebSocketMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(message);
            JsonNode payload = root.path("data");
            if (payload.isMissingNode() || payload.isNull()) {
                payload = root;
            }
            String eventType = payload.path("e").asText("");
            if ("24hrTicker".equals(eventType)) {
                handleTickerEvent(payload);
                return;
            }
            if ("kline".equals(eventType)) {
                handleKlineEvent(payload);
            }
        } catch (Exception ex) {
            log.warn("处理长连接消息失败", ex);
        }
    }

    private void handleTickerEvent(JsonNode payload) {
        String symbol = normalizeSymbolOrNull(payload.path("s").asText(""));
        String priceText = payload.path("c").asText("");
        if (!StringUtils.hasText(symbol) || !StringUtils.hasText(priceText)) {
            return;
        }
        try {
            cacheQuote(buildQuote(symbol, new BigDecimal(priceText)));
        } catch (Exception ex) {
            log.warn("解析Ticker事件失败, 标的={}", symbol, ex);
        }
    }

    private void handleKlineEvent(JsonNode payload) {
        JsonNode klineNode = payload.path("k");
        if (klineNode.isMissingNode() || !klineNode.isObject()) {
            return;
        }
        String symbol = normalizeSymbolOrNull(payload.path("s").asText(klineNode.path("s").asText("")));
        String timeframe = normalizeTimeframe(klineNode.path("i").asText(""));
        if (!StringUtils.hasText(symbol) || !StringUtils.hasText(timeframe)) {
            return;
        }

        ArrayNode incomingCandle = buildKlineArrayNode(klineNode);
        if (incomingCandle == null) {
            return;
        }

        String cacheKey = MarketRedisKeys.klineKey(symbol, timeframe);
        synchronized (klineWriteMonitor) {
            String existingJson = stringRedisTemplate.opsForValue().get(cacheKey);
            String incomingJson = incomingCandle.toString();
            String mergedJson = mergeKlineJson(existingJson, "[" + incomingJson + "]", safeHistoryLimit());
            stringRedisTemplate.opsForValue().set(cacheKey, mergedJson);
        }

        String closePrice = klineNode.path("c").asText("");
        if (StringUtils.hasText(closePrice)) {
            try {
                cacheQuote(buildQuote(symbol, new BigDecimal(closePrice)));
            } catch (Exception ex) {
                log.warn("解析K线收盘价失败, 标的={}", symbol, ex);
            }
        }
    }

    private ArrayNode buildKlineArrayNode(JsonNode klineNode) {
        if (!klineNode.isObject()) {
            return null;
        }
        ArrayNode candle = objectMapper.createArrayNode();
        candle.add(klineNode.path("t").asLong());
        candle.add(klineNode.path("o").asText("0"));
        candle.add(klineNode.path("h").asText("0"));
        candle.add(klineNode.path("l").asText("0"));
        candle.add(klineNode.path("c").asText("0"));
        candle.add(klineNode.path("v").asText("0"));
        candle.add(klineNode.path("T").asLong());
        candle.add(klineNode.path("q").asText("0"));
        candle.add(klineNode.path("n").asInt(0));
        candle.add(klineNode.path("V").asText("0"));
        candle.add(klineNode.path("Q").asText("0"));
        candle.add(klineNode.path("B").asText("0"));
        return candle;
    }

    private String buildWsUrl(List<String> symbols, List<String> timeframes) {
        List<String> streams = new ArrayList<>(symbols.size() * (1 + timeframes.size()));
        for (String symbol : symbols) {
            String lowerSymbol = symbol.toLowerCase(Locale.ROOT);
            streams.add(lowerSymbol + "@ticker");
            for (String timeframe : timeframes) {
                streams.add(lowerSymbol + "@kline_" + timeframe.toLowerCase(Locale.ROOT));
            }
        }
        String base = binanceMarketProperties.getWsBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + WS_COMBINED_STREAM_PATH + String.join("/", streams);
    }

    private List<String> loadActiveSymbols() {
        List<AssetInfo> activeAssets = assetInfoMapper.selectByAssetTypeAndStatus(ASSET_TYPE_CRYPTO, 1);
        if (activeAssets == null || activeAssets.isEmpty()) {
            return List.of();
        }
        List<String> symbols = new ArrayList<>(activeAssets.size());
        for (AssetInfo asset : activeAssets) {
            if (!StringUtils.hasText(asset.getSymbol())) {
                continue;
            }
            symbols.add(asset.getSymbol().trim().toUpperCase(Locale.ROOT));
        }
        return symbols;
    }

    private void clearMarketCache() {
        deleteByPattern(MarketRedisKeys.pricePattern());
        deleteByPattern(MarketRedisKeys.klinePattern());
    }

    private void deleteByPattern(String pattern) {
        if (!StringUtils.hasText(pattern)) {
            return;
        }
        Set<String> keys = new HashSet<>();
        RedisSerializer<String> serializer = stringRedisTemplate.getStringSerializer();
        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(200)
                    .build();
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    String key = serializer.deserialize(cursor.next());
                    if (StringUtils.hasText(key)) {
                        keys.add(key);
                    }
                }
            } catch (Exception ex) {
                log.warn("扫描缓存键失败, 匹配模式={}", pattern, ex);
            }
            return null;
        });
        if (!keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    private long parseOpenTime(JsonNode node) {
        if (node == null || !node.isArray() || node.size() < 1) {
            return -1L;
        }
        return node.get(0).asLong(-1L);
    }

    private Long extractLastKlineTime(String klineJson) {
        if (!StringUtils.hasText(klineJson)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(klineJson);
            if (!root.isArray() || root.size() == 0) {
                return null;
            }
            JsonNode last = root.get(root.size() - 1);
            if (!last.isArray()) {
                return null;
            }
            if (last.size() > 6) {
                return last.get(6).asLong();
            }
            return last.get(0).asLong();
        } catch (Exception ex) {
            log.warn("提取最后K线时间失败", ex);
            return null;
        }
    }

    private MarketQuoteResponse buildQuote(String symbol, BigDecimal price) {
        MarketQuoteResponse response = new MarketQuoteResponse();
        response.setSymbol(symbol);
        response.setPrice(price);
        response.setUpdateTime(System.currentTimeMillis());
        return response;
    }

    private void cacheQuote(MarketQuoteResponse quoteResponse) {
        try {
            if (quoteResponse == null || !StringUtils.hasText(quoteResponse.getSymbol()) || quoteResponse.getPrice() == null) {
                return;
            }
            String cacheKey = MarketRedisKeys.priceKey(quoteResponse.getSymbol());
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            BigDecimal oldPrice = parseCachedQuotePrice(cached);
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(quoteResponse));
            if (oldPrice == null || oldPrice.compareTo(quoteResponse.getPrice()) != 0) {
                marketPriceChangeProducer.sendPriceChange(
                        quoteResponse.getSymbol(),
                        quoteResponse.getPrice(),
                        quoteResponse.getUpdateTime());
            }
        } catch (Exception ex) {
            log.warn("缓存行情失败, 标的={}", quoteResponse.getSymbol(), ex);
        }
    }

    private BigDecimal parseCachedQuotePrice(String cached) {
        if (!StringUtils.hasText(cached)) {
            return null;
        }
        try {
            MarketQuoteResponse quote = objectMapper.readValue(cached, MarketQuoteResponse.class);
            return quote == null ? null : quote.getPrice();
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeSymbol(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            throw new IllegalArgumentException("symbol is required");
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeSymbolOrNull(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            return null;
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeTimeframe(String timeframe) {
        String normalized = normalizeTimeframeOrNull(timeframe);
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        String defaultTimeframe = normalizeTimeframeOrNull(binanceMarketProperties.getKlineInterval());
        return StringUtils.hasText(defaultTimeframe) ? defaultTimeframe : "1m";
    }

    private String normalizeTimeframeOrNull(String timeframe) {
        if (!StringUtils.hasText(timeframe)) {
            return null;
        }
        String trimmed = timeframe.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if ("15min".equals(lower) || "15m".equals(lower)) {
            return "15m";
        }
        if ("1h".equals(lower) || "60m".equals(lower) || "60min".equals(lower)) {
            return "1h";
        }
        if ("4h".equals(lower) || "240m".equals(lower) || "240min".equals(lower)) {
            return "4h";
        }
        if ("1d".equals(lower) || "1day".equals(lower) || "24h".equals(lower)) {
            return "1d";
        }
        if (lower.matches("^[0-9]{1,3}min$")) {
            return lower.substring(0, lower.length() - 3) + "m";
        }
        if (lower.matches("^[0-9]{1,3}[mhdw]$")) {
            return lower;
        }
        if (trimmed.matches("^[0-9]{1,2}M$")) {
            return trimmed;
        }
        return null;
    }

    private List<String> resolveTrackedTimeframes() {
        Set<String> resolved = new LinkedHashSet<>();
        addTimeframe(resolved, normalizeTimeframeOrNull(binanceMarketProperties.getKlineInterval()));

        List<String> configured = binanceMarketProperties.getKlineIntervals();
        if (configured != null) {
            for (String item : configured) {
                addTimeframe(resolved, normalizeTimeframeOrNull(item));
            }
        }

        // Always keep core chart intervals in realtime cache.
        addTimeframe(resolved, "15m");
        addTimeframe(resolved, "1h");
        addTimeframe(resolved, "4h");
        addTimeframe(resolved, "1d");

        if (resolved.isEmpty()) {
            return List.of("15m", "1h", "4h", "1d");
        }
        return List.copyOf(resolved);
    }

    private void addTimeframe(Set<String> container, String timeframe) {
        if (!StringUtils.hasText(timeframe) || container == null) {
            return;
        }
        container.add(timeframe);
    }

    private int safeHistoryLimit() {
        Integer historyLimit = binanceMarketProperties.getHistoryLimit();
        if (historyLimit == null || historyLimit <= 0) {
            return 200;
        }
        return Math.min(historyLimit, 1000);
    }

    private long safeReconnectDelayMs() {
        Long wsReconnectDelayMs = binanceMarketProperties.getWsReconnectDelayMs();
        if (wsReconnectDelayMs == null || wsReconnectDelayMs <= 0) {
            return 3000L;
        }
        return wsReconnectDelayMs;
    }

    private static final class CandleSnapshot {
        private long openTime;
        private long closeTime;
        private String open;
        private String high;
        private String low;
        private BigDecimal close;
        private String closeRaw;
        private String volume;
        private String quoteVolume;
        private int tradeCount;
        private String takerBuyBaseVolume;
        private String takerBuyQuoteVolume;
        private String ignore;
    }

    private final class BinanceStreamListener implements WebSocket.Listener {

        private final long session;
        private final StringBuilder buffer = new StringBuilder();

        private BinanceStreamListener(long session) {
            this.session = session;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                handleWebSocketMessage(buffer.toString());
                buffer.setLength(0);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (!shuttingDown && wsSessionId.get() == session) {
                log.warn("Binance长连接已关闭, 会话={}, 状态码={}, 原因={}", session, statusCode, reason);
                scheduleReconnect(session);
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            if (!shuttingDown && wsSessionId.get() == session) {
                log.error("Binance长连接异常, 会话={}", session, error);
                scheduleReconnect(session);
            }
        }
    }

    private static final class NamedThreadFactory implements ThreadFactory {

        private final String threadName;

        private NamedThreadFactory(String threadName) {
            this.threadName = threadName;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        }
    }
}
