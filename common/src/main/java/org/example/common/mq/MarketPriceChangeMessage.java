package org.example.common.mq;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Message payload for market latest price change event.
 */
@Data
public class MarketPriceChangeMessage {

    private String symbol;

    private BigDecimal price;

    private Long updateTime;
}
