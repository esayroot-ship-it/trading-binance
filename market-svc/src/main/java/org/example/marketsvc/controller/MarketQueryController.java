package org.example.marketsvc.controller;

import lombok.RequiredArgsConstructor;
import org.example.common.entity.AssetInfo;
import org.example.marketsvc.dto.MarketKlineResponse;
import org.example.marketsvc.dto.MarketQuoteResponse;
import org.example.marketsvc.dto.MarketWsKlineSnapshotResponse;
import org.example.marketsvc.service.MarketDataService;
import org.springframework.web.bind.annotation.*;
import tools.R;

import java.util.List;

/**
 * 行情查询接口。
 */
@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketQueryController {

    private final MarketDataService marketDataService;

    /**
     * 查询所有资产信息。
     */
    @GetMapping("/assets")
    public R<List<AssetInfo>> listAssets() {
        return R.ok("query success", marketDataService.listActiveAssets());
    }

    /**
     * 获取资产行情。
     */
    @GetMapping("/price/{symbol}")
    public R<MarketQuoteResponse> getQuote(@PathVariable String symbol) {
        try {
            MarketQuoteResponse response = marketDataService.getQuote(symbol);
            if (response == null) {
                return R.fail("quote not available");
            }
            return R.ok("query success", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 获取资产K线数据。
     */
    @GetMapping("/kline/{symbol}")
    public R<MarketKlineResponse> getKline(
            @PathVariable String symbol,
            @RequestParam(required = false) String timeframe) {
        try {
            MarketKlineResponse response = marketDataService.getKline(symbol, timeframe);
            if (response == null) {
                return R.fail("kline not available");
            }
            return R.ok("query success", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    /**
     * 获取资产前端价格快照。
     */
    @GetMapping("/frontend/price/{symbol}")
    public R<MarketWsKlineSnapshotResponse> getFrontendPriceSnapshot(
            @PathVariable String symbol,
            @RequestParam(required = false) String timeframe) {
        try {
            MarketWsKlineSnapshotResponse response = marketDataService.getFrontendPriceSnapshot(symbol, timeframe);
            if (response == null) {
                return R.fail("price snapshot not available");
            }
            return R.ok("query success", response);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }
}
