package org.example.tradingsvc.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 市场价格数据传输对象。
 */
@Data
public class MarketQuoteDTO {

    /** 交易标的代码。 */
    private String symbol;

    /** 最新价格。 */
    private BigDecimal price;

    /** 行情更新时间戳（毫秒）。 */
    private Long updateTime;
}