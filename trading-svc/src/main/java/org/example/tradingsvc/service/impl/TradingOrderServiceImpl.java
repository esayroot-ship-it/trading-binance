package org.example.tradingsvc.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.DTO.TradingPlatformDTO;
import org.example.common.entity.TradeOrder;
import org.example.common.entity.TradePosition;
import org.example.common.mq.OrderTriggerNotifyMessage;
import org.example.tradingsvc.constant.TradingConstants;
import org.example.tradingsvc.dto.MarketQuoteDTO;
import org.example.tradingsvc.mapper.TradeOrderMapper;
import org.example.tradingsvc.mq.OrderTriggerNotifyProducer;
import org.example.tradingsvc.mapper.TradePositionMapper;
import org.example.tradingsvc.service.TradingAccountService;
import org.example.tradingsvc.service.TradingOrderService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.R;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradingOrderServiceImpl implements TradingOrderService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(8, RoundingMode.DOWN);
    private static final int MAX_LEVERAGE = 125;
    private static final String ORDER_TRIGGER_MSG_TYPE = "ORDER_TRIGGER";

    private final TradeOrderMapper tradeOrderMapper;
    private final TradePositionMapper tradePositionMapper;
    private final TradingAccountService tradingAccountService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final OrderTriggerNotifyProducer orderTriggerNotifyProducer;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public R<TradingPlatformDTO.OrderDetail> placeOrder(Long userId, TradingPlatformDTO.PlaceOrderRequest request) {
        return placeOrderInternal(userId, request, null);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public R<TradingPlatformDTO.OrderDetail> addPosition(Long userId, TradingPlatformDTO.PlaceOrderRequest request) {
        return placeOrderInternal(userId, request, TradingConstants.ACTION_OPEN);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public R<TradingPlatformDTO.OrderDetail> closePosition(Long userId, TradingPlatformDTO.PlaceOrderRequest request) {
        return placeOrderInternal(userId, request, TradingConstants.ACTION_CLOSE);
    }

    @Override
    public R<List<TradingPlatformDTO.OrderDetail>> listOrders(Long userId) {
        if (userId == null || userId <= 0) {
            return R.fail("userId is required");
        }
        try {
            List<TradeOrder> orders = tradeOrderMapper.selectByUserId(userId);
            List<TradingPlatformDTO.OrderDetail> result = new ArrayList<>();
            if (orders != null) {
                for (TradeOrder order : orders) {
                    result.add(toOrderDetail(order));
                }
            }
            return R.ok("query success", result);
        } catch (Exception ex) {
            log.error("查询订单列表失败，用户编号={}", userId, ex);
            return R.fail("query orders failed");
        }
    }

    @Override
    public R<List<TradingPlatformDTO.PositionDetail>> listPositions(Long userId) {
        if (userId == null || userId <= 0) {
            return R.fail("userId is required");
        }
        try {
            List<TradePosition> positions = tradePositionMapper.selectByUserIdAndStatus(
                    userId,
                    TradingConstants.POSITION_STATUS_HOLDING);
            List<TradingPlatformDTO.PositionDetail> result = new ArrayList<>();
            if (positions != null) {
                for (TradePosition position : positions) {
                    result.add(toPositionDetail(position));
                }
            }
            return R.ok("query success", result);
        } catch (Exception ex) {
            log.error("查询持仓列表失败，用户编号={}", userId, ex);
            return R.fail("query positions failed");
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public R<String> cancelOrder(Long userId, String orderNo) {
        if (userId == null || userId <= 0 || !StringUtils.hasText(orderNo)) {
            return R.fail("userId and orderNo are required");
        }
        TradeOrder order = tradeOrderMapper.selectByOrderNo(orderNo.trim());
        if (order == null || !userId.equals(order.getUserId())) {
            return R.fail("order not found");
        }
        if (!TradingConstants.ORDER_STATUS_PENDING.equals(order.getStatus())) {
            return R.fail("only pending order can be canceled");
        }

        int updated = tradeOrderMapper.updateStatusByOrderNoAndUserId(
                order.getOrderNo(),
                userId,
                TradingConstants.ORDER_STATUS_PENDING,
                TradingConstants.ORDER_STATUS_CANCELED,
                LocalDateTime.now());
        if (updated <= 0) {
            return R.fail("cancel order failed");
        }

        BigDecimal reservedMargin = safeAmount(order.getRealizedPnl());
        if (TradingConstants.ACTION_OPEN.equals(order.getAction()) && reservedMargin.compareTo(BigDecimal.ZERO) > 0) {
            tradingAccountService.applyAccountDelta(
                    userId,
                    reservedMargin,
                    reservedMargin.negate(),
                    "UNFREEZE_ORDER",
                    order.getOrderNo(),
                    "cancel pending open order and unfreeze margin");
        }
        return R.ok("cancel order success", "ok");
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void onPriceChanged(String symbol, BigDecimal latestPrice, Long updateTime) {
        if (!StringUtils.hasText(symbol) || latestPrice == null) {
            return;
        }
        String safeSymbol = normalizeText(symbol);
        BigDecimal redisLatestPrice = fetchLatestPriceFromCache(safeSymbol);
        BigDecimal safeLatestPrice = redisLatestPrice != null
                ? redisLatestPrice
                : safeAmount(latestPrice);
        List<TradeOrder> pendingOrders = tradeOrderMapper.selectPendingBySymbol(safeSymbol);
        if (pendingOrders == null || pendingOrders.isEmpty()) {
            return;
        }
        for (TradeOrder order : pendingOrders) {
            if (!shouldTrigger(order, safeLatestPrice)) {
                continue;
            }
            fillPendingOrder(order, safeLatestPrice);
        }
    }

    private R<TradingPlatformDTO.OrderDetail> placeOrderInternal(
            Long userId,
            TradingPlatformDTO.PlaceOrderRequest request,
            String forceAction) {
        if (userId == null || userId <= 0) {
            return R.fail("userId is required");
        }
        if (request == null) {
            return R.fail("request is required");
        }
        if (!StringUtils.hasText(request.getSymbol())
                || !StringUtils.hasText(request.getDirection())
                || !StringUtils.hasText(request.getOrderType())
                || request.getEntrustQuantity() == null
                || request.getEntrustQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return R.fail("symbol/direction/orderType/entrustQuantity are required");
        }

        String symbol = normalizeText(request.getSymbol());
        String direction = normalizeText(request.getDirection());
        String action = StringUtils.hasText(forceAction)
                ? normalizeText(forceAction)
                : normalizeText(request.getAction());
        String orderType = normalizeText(request.getOrderType());
        if (!TradingConstants.DIRECTION_LONG.equals(direction) && !TradingConstants.DIRECTION_SHORT.equals(direction)) {
            return R.fail("direction must be LONG or SHORT");
        }
        if (!TradingConstants.ACTION_OPEN.equals(action) && !TradingConstants.ACTION_CLOSE.equals(action)) {
            return R.fail("action must be OPEN or CLOSE");
        }

        BigDecimal quantity = safeAmount(request.getEntrustQuantity());
        int leverage = normalizeLeverage(request.getLeverage());
        TradeOrder order = buildOrder(userId, request, symbol, direction, action, orderType, quantity);
        if (TradingConstants.ORDER_TYPE_MARKET.equals(orderType)) {
            return handleMarketOrder(order, quantity, leverage);
        }
        return handlePendingOrder(order, quantity, leverage);
    }

    private R<TradingPlatformDTO.OrderDetail> handleMarketOrder(TradeOrder order, BigDecimal quantity, int leverage) {
        BigDecimal latestPrice = fetchLatestPriceFromCache(order.getSymbol());
        if (latestPrice == null) {
            return R.fail("latest price not found in cache");
        }

        order.setEntrustPrice(latestPrice);
        order.setFilledQuantity(quantity);
        order.setAvgFilledPrice(latestPrice);
        order.setStatus(TradingConstants.ORDER_STATUS_FILLED);

        if (TradingConstants.ACTION_OPEN.equals(order.getAction())) {
            int effectiveLeverage = resolveOpenLeverage(order.getUserId(), order.getSymbol(), order.getDirection(), leverage);
            BigDecimal margin = calcMargin(latestPrice, quantity, effectiveLeverage);
            tradingAccountService.applyAccountDelta(
                    order.getUserId(),
                    margin.negate(),
                    ZERO,
                    "OPEN_MARKET_MARGIN",
                    order.getOrderNo(),
                    "market open order margin deduction");
            applyOpenPosition(order, quantity, latestPrice, effectiveLeverage, margin);
            order.setRealizedPnl(ZERO);
        } else {
            ClosePreview preview = buildClosePreview(order.getUserId(), order.getSymbol(), order.getDirection(), quantity, latestPrice);
            applyClosePreview(preview, order.getOrderNo(), "CLOSE_MARKET_SETTLE");
            order.setRealizedPnl(preview.realizedPnl);
        }

        if (tradeOrderMapper.insert(order) <= 0) {
            return R.fail("create market order failed");
        }
        return R.ok("place market order success", toOrderDetail(order));
    }

    private R<TradingPlatformDTO.OrderDetail> handlePendingOrder(TradeOrder order, BigDecimal quantity, int leverage) {
        String orderType = order.getOrderType();
        if (!isSupportedPendingType(orderType)) {
            return R.fail("unsupported pending order type");
        }
        if (TradingConstants.ORDER_TYPE_LIMIT.equals(orderType) && order.getEntrustPrice() == null) {
            return R.fail("entrustPrice is required for LIMIT");
        }
        if (!TradingConstants.ORDER_TYPE_LIMIT.equals(orderType) && order.getTriggerPrice() == null) {
            return R.fail("triggerPrice is required for trigger order");
        }

        BigDecimal latestPrice = fetchLatestPriceFromCache(order.getSymbol());
        if (latestPrice == null) {
            return R.fail("latest price not found in cache");
        }

        BigDecimal reservedMargin = ZERO;
        if (TradingConstants.ACTION_OPEN.equals(order.getAction())) {
            int effectiveLeverage = resolveOpenLeverage(order.getUserId(), order.getSymbol(), order.getDirection(), leverage);
            BigDecimal reservePrice = resolveReservePrice(order, latestPrice);
            reservedMargin = calcMargin(reservePrice, quantity, effectiveLeverage);
            tradingAccountService.applyAccountDelta(
                    order.getUserId(),
                    reservedMargin.negate(),
                    reservedMargin,
                    "FREEZE_ORDER",
                    order.getOrderNo(),
                    "freeze margin for pending open order");
        } else {
            ensureClosePosition(order.getUserId(), order.getSymbol(), order.getDirection(), quantity);
        }

        order.setStatus(TradingConstants.ORDER_STATUS_PENDING);
        order.setRealizedPnl(reservedMargin);
        if (tradeOrderMapper.insert(order) <= 0) {
            return R.fail("create pending order failed");
        }
        return R.ok("place pending order success", toOrderDetail(order));
    }

    private void fillPendingOrder(TradeOrder order, BigDecimal latestPrice) {
        if (TradingConstants.ACTION_OPEN.equals(order.getAction())) {
            fillPendingOpen(order, latestPrice);
            return;
        }
        fillPendingClose(order, latestPrice);
    }

    private void fillPendingOpen(TradeOrder order, BigDecimal latestPrice) {
        BigDecimal quantity = safeAmount(order.getEntrustQuantity());
        BigDecimal reservedMargin = safeAmount(order.getRealizedPnl());
        int leverage = resolveLeverageForPendingOpen(order, latestPrice, quantity, reservedMargin);
        int updated = tradeOrderMapper.updateFilledIfPending(
                order.getId(),
                quantity,
                latestPrice,
                ZERO,
                LocalDateTime.now());
        if (updated <= 0) {
            return;
        }

        if (reservedMargin.compareTo(BigDecimal.ZERO) > 0) {
            tradingAccountService.applyAccountDelta(
                    order.getUserId(),
                    ZERO,
                    reservedMargin.negate(),
                    "PENDING_OPEN_FILLED",
                    order.getOrderNo(),
                    "pending open order filled, consume frozen margin");
        }
        applyOpenPosition(order, quantity, latestPrice, leverage, reservedMargin);
        sendOrderTriggeredNotify(order, latestPrice, quantity);
    }

    private void fillPendingClose(TradeOrder order, BigDecimal latestPrice) {
        BigDecimal quantity = safeAmount(order.getEntrustQuantity());
        ClosePreview preview;
        try {
            preview = buildClosePreview(order.getUserId(), order.getSymbol(), order.getDirection(), quantity, latestPrice);
        } catch (IllegalArgumentException ex) {
            cancelInvalidPendingClose(order);
            return;
        }
        int updated = tradeOrderMapper.updateFilledIfPending(
                order.getId(),
                quantity,
                latestPrice,
                preview.realizedPnl,
                LocalDateTime.now());
        if (updated <= 0) {
            return;
        }
        applyClosePreview(preview, order.getOrderNo(), "CLOSE_PENDING_SETTLE");
        sendOrderTriggeredNotify(order, latestPrice, quantity);
    }

    private void cancelInvalidPendingClose(TradeOrder order) {
        tradeOrderMapper.updateStatusByOrderNoAndUserId(
                order.getOrderNo(),
                order.getUserId(),
                TradingConstants.ORDER_STATUS_PENDING,
                TradingConstants.ORDER_STATUS_CANCELED,
                LocalDateTime.now());
    }

    private void applyOpenPosition(TradeOrder order,
                                   BigDecimal quantity,
                                   BigDecimal fillPrice,
                                   int leverage,
                                   BigDecimal marginDelta) {
        TradePosition position = tradePositionMapper.selectActiveByUserSymbolDirection(
                order.getUserId(),
                order.getSymbol(),
                order.getDirection());
        LocalDateTime now = LocalDateTime.now();
        BigDecimal safeQuantity = safeAmount(quantity);
        BigDecimal safeMargin = safeAmount(marginDelta);

        if (position == null) {
            TradePosition entity = new TradePosition();
            entity.setUserId(order.getUserId());
            entity.setSymbol(order.getSymbol());
            entity.setAssetType(order.getAssetType());
            entity.setDirection(order.getDirection());
            entity.setLeverage(leverage);
            entity.setHoldQuantity(safeQuantity);
            entity.setAvailableQuantity(safeQuantity);
            entity.setOpenPrice(fillPrice);
            entity.setMargin(safeMargin);
            entity.setLiquidationPrice(calcLiquidationPrice(fillPrice, leverage, order.getDirection()));
            entity.setStatus(TradingConstants.POSITION_STATUS_HOLDING);
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            tradePositionMapper.insert(entity);
            return;
        }

        if (position.getLeverage() != null && position.getLeverage() > 0 && position.getLeverage() != leverage) {
            throw new IllegalArgumentException("leverage mismatch with existing position");
        }
        BigDecimal oldHold = safeAmount(position.getHoldQuantity());
        BigDecimal oldMargin = safeAmount(position.getMargin());
        BigDecimal oldOpenPrice = safeAmount(position.getOpenPrice());
        BigDecimal newHold = oldHold.add(safeQuantity);
        if (newHold.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("invalid hold quantity");
        }
        BigDecimal weightedOpen = oldOpenPrice.multiply(oldHold)
                .add(fillPrice.multiply(safeQuantity))
                .divide(newHold, 8, RoundingMode.HALF_UP);
        BigDecimal newMargin = oldMargin.add(safeMargin);
        position.setHoldQuantity(newHold);
        position.setAvailableQuantity(newHold);
        position.setOpenPrice(weightedOpen);
        position.setMargin(newMargin);
        int effectiveLeverage = position.getLeverage() == null || position.getLeverage() <= 0
                ? leverage
                : position.getLeverage();
        position.setLeverage(effectiveLeverage);
        position.setLiquidationPrice(calcLiquidationPrice(weightedOpen, effectiveLeverage, position.getDirection()));
        position.setStatus(TradingConstants.POSITION_STATUS_HOLDING);
        position.setUpdateTime(now);
        tradePositionMapper.updateById(position);
    }

    private ClosePreview buildClosePreview(Long userId,
                                           String symbol,
                                           String direction,
                                           BigDecimal closeQuantity,
                                           BigDecimal fillPrice) {
        ensureClosePosition(userId, symbol, direction, closeQuantity);
        TradePosition position = tradePositionMapper.selectActiveByUserSymbolDirection(userId, symbol, direction);
        if (position == null) {
            throw new IllegalArgumentException("position not found");
        }
        BigDecimal oldHold = safeAmount(position.getHoldQuantity());
        BigDecimal oldMargin = safeAmount(position.getMargin());
        BigDecimal safeClose = safeAmount(closeQuantity);
        BigDecimal marginRelease = oldMargin.multiply(safeClose).divide(oldHold, 8, RoundingMode.HALF_UP);
        BigDecimal realizedPnl = calcRealizedPnl(direction, safeAmount(position.getOpenPrice()), fillPrice, safeClose);
        BigDecimal newHold = oldHold.subtract(safeClose).setScale(8, RoundingMode.HALF_UP);
        BigDecimal newMargin = oldMargin.subtract(marginRelease).setScale(8, RoundingMode.HALF_UP);
        if (newMargin.compareTo(BigDecimal.ZERO) < 0) {
            newMargin = ZERO;
        }
        return new ClosePreview(position, marginRelease, realizedPnl, newHold, newMargin);
    }

    private void applyClosePreview(ClosePreview preview, String orderNo, String transType) {
        TradePosition position = preview.position;
        LocalDateTime now = LocalDateTime.now();
        if (preview.newHold.compareTo(BigDecimal.ZERO) <= 0) {
            position.setHoldQuantity(ZERO);
            position.setAvailableQuantity(ZERO);
            position.setOpenPrice(ZERO);
            position.setMargin(ZERO);
            position.setLiquidationPrice(null);
            position.setStatus(TradingConstants.POSITION_STATUS_CLOSED);
        } else {
            position.setHoldQuantity(preview.newHold);
            position.setAvailableQuantity(preview.newHold);
            position.setMargin(preview.newMargin);
            position.setLiquidationPrice(calcLiquidationPrice(
                    safeAmount(position.getOpenPrice()),
                    position.getLeverage() == null ? 1 : position.getLeverage(),
                    position.getDirection()));
            position.setStatus(TradingConstants.POSITION_STATUS_HOLDING);
        }
        position.setUpdateTime(now);
        tradePositionMapper.updateById(position);

        BigDecimal accountDelta = preview.marginRelease.add(preview.realizedPnl).setScale(8, RoundingMode.HALF_UP);
        tradingAccountService.applyAccountDelta(
                position.getUserId(),
                accountDelta,
                ZERO,
                transType,
                orderNo,
                "position close settlement");
    }

    private void ensureClosePosition(Long userId, String symbol, String direction, BigDecimal closeQuantity) {
        TradePosition position = tradePositionMapper.selectActiveByUserSymbolDirection(userId, symbol, direction);
        if (position == null) {
            throw new IllegalArgumentException("position not found");
        }
        BigDecimal hold = safeAmount(position.getHoldQuantity());
        BigDecimal close = safeAmount(closeQuantity);
        if (close.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("close quantity must be greater than 0");
        }
        if (hold.compareTo(close) < 0) {
            throw new IllegalArgumentException("close quantity exceeds hold quantity");
        }
    }

    private boolean shouldTrigger(TradeOrder order, BigDecimal latestPrice) {
        if (order == null || latestPrice == null) {
            return false;
        }
        String orderType = normalizeText(order.getOrderType());
        String direction = normalizeText(order.getDirection());
        BigDecimal triggerPrice = safeAmount(order.getTriggerPrice());
        BigDecimal entrustPrice = safeAmount(order.getEntrustPrice());

        if (TradingConstants.ORDER_TYPE_LIMIT.equals(orderType)) {
            if (entrustPrice.compareTo(BigDecimal.ZERO) <= 0) {
                return false;
            }
            if (TradingConstants.DIRECTION_LONG.equals(direction)) {
                return latestPrice.compareTo(entrustPrice) <= 0;
            }
            return latestPrice.compareTo(entrustPrice) >= 0;
        }
        if (TradingConstants.ORDER_TYPE_STOP_LOSS.equals(orderType)) {
            if (triggerPrice.compareTo(BigDecimal.ZERO) <= 0) {
                return false;
            }
            if (TradingConstants.DIRECTION_LONG.equals(direction)) {
                return latestPrice.compareTo(triggerPrice) <= 0;
            }
            return latestPrice.compareTo(triggerPrice) >= 0;
        }
        if (TradingConstants.ORDER_TYPE_TAKE_PROFIT.equals(orderType)) {
            if (triggerPrice.compareTo(BigDecimal.ZERO) <= 0) {
                return false;
            }
            if (TradingConstants.DIRECTION_LONG.equals(direction)) {
                return latestPrice.compareTo(triggerPrice) >= 0;
            }
            return latestPrice.compareTo(triggerPrice) <= 0;
        }
        return false;
    }

    private int resolveOpenLeverage(Long userId, String symbol, String direction, int leverage) {
        TradePosition position = tradePositionMapper.selectActiveByUserSymbolDirection(userId, symbol, direction);
        if (position == null) {
            return leverage;
        }
        Integer existing = position.getLeverage();
        if (existing == null || existing <= 0) {
            return leverage;
        }
        if (leverage > 0 && leverage != existing) {
            throw new IllegalArgumentException("leverage mismatch with existing position");
        }
        return existing;
    }

    private int resolveLeverageForPendingOpen(TradeOrder order,
                                              BigDecimal fillPrice,
                                              BigDecimal quantity,
                                              BigDecimal reservedMargin) {
        TradePosition position = tradePositionMapper.selectActiveByUserSymbolDirection(
                order.getUserId(),
                order.getSymbol(),
                order.getDirection());
        if (position != null && position.getLeverage() != null && position.getLeverage() > 0) {
            return position.getLeverage();
        }
        if (reservedMargin == null || reservedMargin.compareTo(BigDecimal.ZERO) <= 0) {
            return 1;
        }
        BigDecimal notional = safeAmount(fillPrice).multiply(safeAmount(quantity));
        if (notional.compareTo(BigDecimal.ZERO) <= 0) {
            return 1;
        }
        int leverage = notional.divide(reservedMargin, 0, RoundingMode.HALF_UP).intValue();
        if (leverage < 1) {
            return 1;
        }
        return Math.min(leverage, MAX_LEVERAGE);
    }

    private BigDecimal fetchLatestPriceFromCache(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            return null;
        }
        String cacheKey = TradingConstants.MARKET_PRICE_CACHE_PREFIX + normalizeText(symbol);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (!StringUtils.hasText(cached)) {
            return null;
        }
        try {
            MarketQuoteDTO quote = objectMapper.readValue(cached, MarketQuoteDTO.class);
            if (quote == null || quote.getPrice() == null) {
                return null;
            }
            return safeAmount(quote.getPrice());
        } catch (Exception ex) {
            log.warn("解析市场行情缓存失败，标的={}", symbol, ex);
            return null;
        }
    }

    private BigDecimal calcMargin(BigDecimal price, BigDecimal quantity, int leverage) {
        if (price == null || quantity == null || leverage <= 0) {
            throw new IllegalArgumentException("invalid margin params");
        }
        return safeAmount(price.multiply(quantity)
                .divide(BigDecimal.valueOf(leverage), 8, RoundingMode.HALF_UP));
    }

    private BigDecimal calcRealizedPnl(String direction, BigDecimal openPrice, BigDecimal closePrice, BigDecimal quantity) {
        BigDecimal pnlPerUnit;
        if (TradingConstants.DIRECTION_SHORT.equals(direction)) {
            pnlPerUnit = openPrice.subtract(closePrice);
        } else {
            pnlPerUnit = closePrice.subtract(openPrice);
        }
        return safeAmount(pnlPerUnit.multiply(quantity));
    }

    private BigDecimal resolveReservePrice(TradeOrder order, BigDecimal latestPrice) {
        if (TradingConstants.ORDER_TYPE_LIMIT.equals(order.getOrderType()) && order.getEntrustPrice() != null) {
            return safeAmount(order.getEntrustPrice());
        }
        if (order.getTriggerPrice() != null && order.getTriggerPrice().compareTo(BigDecimal.ZERO) > 0) {
            return safeAmount(order.getTriggerPrice());
        }
        return safeAmount(latestPrice);
    }

    private BigDecimal calcLiquidationPrice(BigDecimal openPrice, int leverage, String direction) {
        if (openPrice == null || openPrice.compareTo(BigDecimal.ZERO) <= 0 || leverage <= 1) {
            return null;
        }
        BigDecimal lv = BigDecimal.valueOf(leverage);
        BigDecimal ratio = BigDecimal.ONE.divide(lv, 8, RoundingMode.HALF_UP);
        BigDecimal value;
        if (TradingConstants.DIRECTION_SHORT.equals(normalizeText(direction))) {
            value = openPrice.multiply(BigDecimal.ONE.add(ratio));
        } else {
            value = openPrice.multiply(BigDecimal.ONE.subtract(ratio));
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return value.setScale(8, RoundingMode.HALF_UP);
    }

    private boolean isSupportedPendingType(String orderType) {
        return TradingConstants.ORDER_TYPE_LIMIT.equals(orderType)
                || TradingConstants.ORDER_TYPE_STOP_LOSS.equals(orderType)
                || TradingConstants.ORDER_TYPE_TAKE_PROFIT.equals(orderType);
    }

    private int normalizeLeverage(Integer leverage) {
        if (leverage == null || leverage <= 0) {
            return 1;
        }
        if (leverage > MAX_LEVERAGE) {
            return MAX_LEVERAGE;
        }
        return leverage;
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal safeAmount(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(8, RoundingMode.HALF_UP);
    }

    private TradeOrder buildOrder(Long userId,
                                  TradingPlatformDTO.PlaceOrderRequest request,
                                  String symbol,
                                  String direction,
                                  String action,
                                  String orderType,
                                  BigDecimal quantity) {
        TradeOrder order = new TradeOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setSymbol(symbol);
        order.setAssetType(StringUtils.hasText(request.getAssetType())
                ? normalizeText(request.getAssetType())
                : TradingConstants.DEFAULT_ASSET_TYPE);
        order.setDirection(direction);
        order.setAction(action);
        order.setOrderType(orderType);
        order.setTriggerPrice(request.getTriggerPrice() == null ? null : safeAmount(request.getTriggerPrice()));
        order.setEntrustPrice(request.getEntrustPrice() == null ? null : safeAmount(request.getEntrustPrice()));
        order.setEntrustQuantity(quantity);
        order.setFilledQuantity(ZERO);
        order.setAvgFilledPrice(ZERO);
        order.setRealizedPnl(ZERO);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        return order;
    }

    private String generateOrderNo() {
        return "OD" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private void sendOrderTriggeredNotify(TradeOrder order, BigDecimal filledPrice, BigDecimal filledQuantity) {
        if (order == null || order.getUserId() == null || order.getUserId() <= 0) {
            return;
        }
        OrderTriggerNotifyMessage message = new OrderTriggerNotifyMessage();
        message.setUserId(order.getUserId());
        message.setOrderNo(order.getOrderNo());
        message.setSymbol(order.getSymbol());
        message.setOrderType(order.getOrderType());
        message.setAction(order.getAction());
        message.setDirection(order.getDirection());
        message.setTriggerPrice(order.getTriggerPrice());
        message.setEntrustPrice(order.getEntrustPrice());
        message.setFilledPrice(safeAmount(filledPrice));
        message.setFilledQuantity(safeAmount(filledQuantity));
        message.setMsgType(ORDER_TRIGGER_MSG_TYPE);
        message.setTitle("Order triggered and filled");
        message.setContent(buildOrderTriggerContent(message));
        message.setCreateTime(LocalDateTime.now());
        orderTriggerNotifyProducer.send(message);
    }

    private String buildOrderTriggerContent(OrderTriggerNotifyMessage message) {
        StringBuilder builder = new StringBuilder(256);
        builder.append("Order ")
                .append(message.getOrderNo())
                .append(" triggered and filled. Symbol ")
                .append(message.getSymbol())
                .append(", direction ")
                .append(message.getDirection())
                .append(", action ")
                .append(message.getAction())
                .append(", type ")
                .append(message.getOrderType())
                .append(", fill price ")
                .append(message.getFilledPrice())
                .append(", fill quantity ")
                .append(message.getFilledQuantity());
        if (message.getTriggerPrice() != null && message.getTriggerPrice().compareTo(BigDecimal.ZERO) > 0) {
            builder.append(", trigger price ").append(message.getTriggerPrice());
        }
        if (message.getEntrustPrice() != null && message.getEntrustPrice().compareTo(BigDecimal.ZERO) > 0) {
            builder.append(", entrust price ").append(message.getEntrustPrice());
        }
        return builder.toString();
    }

    private TradingPlatformDTO.OrderDetail toOrderDetail(TradeOrder order) {
        TradingPlatformDTO.OrderDetail detail = new TradingPlatformDTO.OrderDetail();
        detail.setOrderNo(order.getOrderNo());
        detail.setSymbol(order.getSymbol());
        detail.setStatus(order.getStatus());
        detail.setEntrustQuantity(order.getEntrustQuantity());
        detail.setFilledQuantity(order.getFilledQuantity());
        detail.setAvgFilledPrice(order.getAvgFilledPrice());
        detail.setRealizedPnl(order.getRealizedPnl());
        detail.setUpdateTime(order.getUpdateTime());
        return detail;
    }

    private TradingPlatformDTO.PositionDetail toPositionDetail(TradePosition position) {
        TradingPlatformDTO.PositionDetail detail = new TradingPlatformDTO.PositionDetail();
        detail.setSymbol(position.getSymbol());
        detail.setDirection(position.getDirection());
        detail.setLeverage(position.getLeverage());
        detail.setHoldQuantity(position.getHoldQuantity());
        detail.setAvailableQuantity(position.getAvailableQuantity());
        detail.setOpenPrice(position.getOpenPrice());
        detail.setMargin(position.getMargin());
        detail.setLiquidationPrice(position.getLiquidationPrice());
        return detail;
    }

    private static final class ClosePreview {

        private final TradePosition position;
        private final BigDecimal marginRelease;
        private final BigDecimal realizedPnl;
        private final BigDecimal newHold;
        private final BigDecimal newMargin;

        private ClosePreview(TradePosition position,
                             BigDecimal marginRelease,
                             BigDecimal realizedPnl,
                             BigDecimal newHold,
                             BigDecimal newMargin) {
            this.position = position;
            this.marginRelease = marginRelease;
            this.realizedPnl = realizedPnl;
            this.newHold = newHold;
            this.newMargin = newMargin;
        }
    }
}
