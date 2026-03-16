package org.example.marketsvc.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 实时价格查询响应。
 */
@Data
public class MarketQuoteResponse {

    /** 交易标的代码。 */
    private String symbol;

    /** 最新价格。 */
    private BigDecimal price;

    /**
     * 行情更新时间戳（毫秒）。
     */
    private Long updateTime;
}
