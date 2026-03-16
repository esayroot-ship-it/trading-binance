package org.example.marketsvc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "market.binance")
public class BinanceMarketProperties {

    /**
     * Binance REST base url.
     */
    private String baseUrl = "https://api.binance.com";

    /**
     * Binance websocket base url.
     */
    private String wsBaseUrl = "wss://stream.binance.com:9443";

    /**
     * Default kline interval.
     */
    private String klineInterval = "1m";

    /**
     * Realtime tracked kline intervals.
     */
    private List<String> klineIntervals = new ArrayList<>(List.of("15m", "1h", "4h", "1d"));

    /**
     * Kline history size kept in cache.
     */
    private Integer historyLimit = 200;

    /**
     * Health check interval in milliseconds.
     */
    private Long refreshIntervalMs = 1000L;

    /**
     * Reconnect delay for websocket in milliseconds.
     */
    private Long wsReconnectDelayMs = 3000L;
}
