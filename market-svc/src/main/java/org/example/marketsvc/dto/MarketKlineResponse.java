package org.example.marketsvc.dto;

import lombok.Data;

/**
 * 行情K线查询响应。
 */
@Data
public class MarketKlineResponse {

    /** 交易标的代码。 */
    private String symbol;

    /** K线周期。 */
    private String timeframe;

    /**
     * K线原始 JSON 数组（Binance OHLCV 结构）。
     */
    private String klineJson;

    /** 最后一根K线时间戳（毫秒）。 */
    private Long lastKlineTime;
}
