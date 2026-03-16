package org.example.common.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户资金账户实体，对应 user_account 表。
 */
@Data
public class UserAccount {
    /** 主键ID */
    private Long id;
    /** 用户编号 */
    private Long userId;
    /** 计价货币 */
    private String currency;
    /** 可用余额 */
    private BigDecimal balance;
    /** 冻结余额 */
    private BigDecimal frozenBalance;
    /** 乐观锁版本号 */
    private Integer version;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
