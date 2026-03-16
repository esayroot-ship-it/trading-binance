package org.example.marketsvc.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.marketsvc.service.MarketDataService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataRefreshScheduler {

    private final MarketDataService marketDataService;

    @Scheduled(fixedDelayString = "${market.binance.refresh-interval-ms:1000}")
    public void refreshRealtimeStream() {
        try {
            marketDataService.refreshMarketCache();
        } catch (Exception ex) {
            log.error("市场缓存刷新失败", ex);
        }
    }

    @Scheduled(
            fixedDelayString = "${market.binance.kline-rest-refresh-interval-ms:60000}",
            initialDelayString = "${market.binance.kline-rest-refresh-initial-delay-ms:5000}")
    public void refreshKlineByRest() {
        try {
            marketDataService.refreshKlineCacheByRest();
        } catch (Exception ex) {
            log.error("市场蜡烛图 接口轮询 刷新失败", ex);
        }
    }
}
