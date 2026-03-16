package org.example.tradingsvc.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 账户资金调整请求。
 */
@Data
public class AccountAdjustRequest {

    /**
     * 调整目标：BALANCE（可用余额）或 FROZEN（冻结余额）。
     */
    private String targetType;

    /**
     * 调整方向：INCREASE（增加）或 DECREASE（减少）。
     */
    private String operation;

    /** 调整金额。 */
    private BigDecimal amount;

    /** 资金流水类型。 */
    private String transType;

    /** 关联业务ID（如订单号）。 */
    private String refId;

    /** 备注说明。 */
    private String remark;
}
