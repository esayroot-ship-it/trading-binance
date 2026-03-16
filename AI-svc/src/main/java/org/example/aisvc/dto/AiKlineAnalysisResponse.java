package org.example.aisvc.dto;

import lombok.Data;

/**
 * AI K线分析结果响应。
 */
@Data
public class AiKlineAnalysisResponse {

    /** 交易标的代码。 */
    private String symbol;

    /** K线周期，如 15m/1h/4h/1d。 */
    private String timeframe;

    /** Redis 缓存键。 */
    private String cacheKey;

    /** 本次分析使用的K线数量。 */
    private Integer candleCount;

    /** 最后一根K线时间戳（毫秒）。 */
    private Long lastKlineTime;

    /** AI 输出的简要分析文本。 */
    private String analysis;

    /** 分析生成时间戳（毫秒）。 */
    private Long analyzeTime;
}
