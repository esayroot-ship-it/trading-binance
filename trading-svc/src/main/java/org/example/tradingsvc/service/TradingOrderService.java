package org.example.tradingsvc.service;

import java.math.BigDecimal;
import java.util.List;
import org.example.common.DTO.TradingPlatformDTO;
import tools.R;

public interface TradingOrderService {

    R<TradingPlatformDTO.OrderDetail> placeOrder(Long userId, TradingPlatformDTO.PlaceOrderRequest request);

    R<TradingPlatformDTO.OrderDetail> addPosition(Long userId, TradingPlatformDTO.PlaceOrderRequest request);

    R<TradingPlatformDTO.OrderDetail> closePosition(Long userId, TradingPlatformDTO.PlaceOrderRequest request);

    R<List<TradingPlatformDTO.OrderDetail>> listOrders(Long userId);

    R<List<TradingPlatformDTO.PositionDetail>> listPositions(Long userId);

    R<String> cancelOrder(Long userId, String orderNo);

    void onPriceChanged(String symbol, BigDecimal latestPrice, Long updateTime);
}
