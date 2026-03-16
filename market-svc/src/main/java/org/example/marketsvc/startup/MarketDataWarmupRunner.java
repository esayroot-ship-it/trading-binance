package org.example.marketsvc.startup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.marketsvc.service.MarketDataService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataWarmupRunner implements ApplicationRunner {

    private final MarketDataService marketDataService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            marketDataService.warmupMarketCache();
        } catch (Exception ex) {
            log.error("市场预热失败", ex);
        }
    }
}
