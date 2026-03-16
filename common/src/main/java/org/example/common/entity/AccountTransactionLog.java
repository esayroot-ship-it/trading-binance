package org.example.common.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 资金流水实体，对应 account_transaction_log 表。
 */
@Data
public class AccountTransactionLog {
    /** 主键ID */
    private Long id;
    /** 用户编号 */
    private Long userId;
    /** 币种 */
    private String currency;
    /** 流水类型 */
    private String transType;
    /** 变动金额 */
    private BigDecimal amount;
    /** 变动后余额 */
    private BigDecimal balanceAfter;
    /** 关联业务ID */
    private String refId;
    /** 流水备注 */
    private String remark;
    /** 创建时间 */
    private LocalDateTime createTime;
}
