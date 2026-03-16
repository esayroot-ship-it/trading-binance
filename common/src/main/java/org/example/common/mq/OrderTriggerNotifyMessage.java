package org.example.common.mq;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Message payload for notifying users when pending entrust orders are triggered.
 */
@Data
public class OrderTriggerNotifyMessage {

    private Long userId;

    private String orderNo;

    private String symbol;

    private String orderType;

    private String action;

    private String direction;

    private BigDecimal triggerPrice;

    private BigDecimal entrustPrice;

    private BigDecimal filledPrice;

    private BigDecimal filledQuantity;

    private String title;

    private String content;

    private String msgType;

    private LocalDateTime createTime;
}
