package org.example.marketsvc.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.marketsvc.dto.MarketQuoteResponse;
import org.example.marketsvc.dto.MarketWsKlineSnapshotResponse;
import org.example.marketsvc.service.MarketDataService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketWsPushScheduler {

    private final MarketWsSubscriptionManager subscriptionManager;
    private final MarketDataService marketDataService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${market.ws.push-interval-ms:1000}")
    public void push() {
        for (MarketWsSubscriptionManager.SessionSnapshot snapshot : subscriptionManager.snapshot()) {
            WebSocketSession session = snapshot.session();
            if (session == null || !session.isOpen()) {
                subscriptionManager.remove(snapshot.sessionId());
                continue;
            }
            for (String topic : snapshot.topics()) {
                try {
                    Map<String, Object> payload = buildPayload(snapshot.sessionId(), topic);
                    if (payload != null) {
                        sendJson(session, payload);
                    }
                } catch (Exception ex) {
                    log.warn("市场长连接推送失败，会话编号={}，主题={}", snapshot.sessionId(), topic, ex);
                }
            }
        }
    }

    private Map<String, Object> buildPayload(String sessionId, String topic) {
        MarketWsTopicParser.ParsedTopic parsed = MarketWsTopicParser.parse(topic);
        if (parsed == null) {
            return null;
        }
        long seq = subscriptionManager.nextSeq(sessionId);
        if (seq < 0) {
            return null;
        }
        if (parsed.type() == MarketWsTopicParser.TopicType.QUOTE) {
            return buildQuotePayload(parsed.normalizedTopic(), parsed.symbol(), seq);
        }
        if (parsed.type() == MarketWsTopicParser.TopicType.KLINE) {
            return buildKlinePayload(parsed.normalizedTopic(), parsed.symbol(), parsed.timeframe(), seq);
        }
        return null;
    }

    private Map<String, Object> buildQuotePayload(String topic, String symbol, long seq) {
        MarketQuoteResponse quote = marketDataService.getQuote(symbol);
        if (quote == null || quote.getPrice() == null) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "quote");
        payload.put("arg", topic);
        payload.put("symbol", quote.getSymbol());
        payload.put("price", quote.getPrice());
        payload.put("updateTime", quote.getUpdateTime());
        payload.put("seq", seq);
        return payload;
    }

    private Map<String, Object> buildKlinePayload(String topic, String symbol, String timeframe, long seq) {
        MarketWsKlineSnapshotResponse snapshot = marketDataService.getFrontendPriceSnapshot(symbol, timeframe);
        if (snapshot == null) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "kline");
        payload.put("arg", topic);
        payload.put("stream", snapshot.getStream());
        payload.put("data", snapshot.getData());
        payload.put("klineJson", snapshot.getKlineJson());
        payload.put("currentPrice", snapshot.getCurrentPrice());
        payload.put("twoKlineChangeAmount", snapshot.getTwoKlineChangeAmount());
        payload.put("twoKlineChangePercent", snapshot.getTwoKlineChangePercent());
        payload.put("rising", snapshot.getRising());
        payload.put("updateTime", snapshot.getUpdateTime());
        payload.put("seq", seq);
        return payload;
    }

    private void sendJson(WebSocketSession session, Object payload) {
        try {
            synchronized (session) {
                if (!session.isOpen()) {
                    return;
                }
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            }
        } catch (IOException ex) {
            log.warn("市场长连接推送发送失败，会话编号={}", session.getId(), ex);
            subscriptionManager.remove(session.getId());
            closeQuietly(session);
        } catch (Exception ex) {
            log.warn("市场长连接推送发送失败，会话编号={}", session.getId(), ex);
        }
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            if (session != null && session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (IOException ex) {
            log.debug("忽略长连接关闭异常", ex);
        }
    }
}
