package org.example.tradingsvc.controller;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.example.common.DTO.TradingPlatformDTO;
import org.example.tradingsvc.service.TradingOrderService;
import org.springframework.web.bind.annotation.*;
import tools.R;

import java.util.List;

/**
 * 交易订单接口。
 */
@RestController
@RequestMapping("/api/trading/orders")
@RequiredArgsConstructor
@Slf4j
public class TradingOrderController {

    private final TradingOrderService tradingOrderService;

    /**
     * 创建订单。
     */
    @PostMapping
    public R<TradingPlatformDTO.OrderDetail> placeOrder(@RequestHeader("X-User-Id") Long userId,
                                                        @RequestBody TradingPlatformDTO.PlaceOrderRequest request) {
        try {
            return tradingOrderService.placeOrder(userId, request);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        } catch (Exception ex) {
            log.error("下单失败，用户编号={}", userId, ex);
            return R.fail("place order failed");
        }
    }

    /**
     * 添加仓位。
     */
    @PostMapping("/add-position")
    public R<TradingPlatformDTO.OrderDetail> addPosition(@RequestHeader("X-User-Id") Long userId,
                                                         @RequestBody TradingPlatformDTO.PlaceOrderRequest request) {
        try {
            return tradingOrderService.addPosition(userId, request);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        } catch (Exception ex) {
            log.error("加仓失败，用户编号={}", userId, ex);
            return R.fail("add position failed");
        }
    }

    /**
     * 减仓。
     */
    @PostMapping("/close-position")
    public R<TradingPlatformDTO.OrderDetail> closePosition(@RequestHeader("X-User-Id") Long userId,
                                                           @RequestBody TradingPlatformDTO.PlaceOrderRequest request) {
        try {
            return tradingOrderService.closePosition(userId, request);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        } catch (Exception ex) {
            log.error("平仓失败，用户编号={}", userId, ex);
            return R.fail("close position failed");
        }
    }

    /**
     * 列出所有订单。
     */
    @GetMapping
    public R<List<TradingPlatformDTO.OrderDetail>> listOrders(@RequestHeader("X-User-Id") Long userId) {
        return tradingOrderService.listOrders(userId);
    }

    /**
     * 列出所有持仓。
     */
    @GetMapping("/positions")
    public R<List<TradingPlatformDTO.PositionDetail>> listPositions(@RequestHeader("X-User-Id") Long userId) {
        return tradingOrderService.listPositions(userId);
    }

    /**
     * 取消订单。
     */
    @DeleteMapping("/{orderNo}")
    public R<String> cancelOrder(@RequestHeader("X-User-Id") Long userId, @PathVariable String orderNo) {
        try {
            return tradingOrderService.cancelOrder(userId, orderNo);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        } catch (Exception ex) {
            log.error("撤单失败，用户编号={}，订单号={}", userId, orderNo, ex);
            return R.fail("cancel order failed");
        }
    }
}
