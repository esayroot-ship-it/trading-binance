package org.example.common.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 委托订单实体，对应 trade_order 表。
 */
@Data
public class TradeOrder {
    /** 主键ID */
    private Long id;
    /** 全局唯一订单号 */
    private String orderNo;
    /** 用户编号 */
    private Long userId;
    /** 资产代码 */
    private String symbol;
    /** 资产类型 */
    private String assetType;
    /** 方向 */
    private String direction;
    /** 动作 */
    private String action;
    /** 订单类型 */
    private String orderType;
    /** 条件触发价 */
    private BigDecimal triggerPrice;
    /** 委托价格 */
    private BigDecimal entrustPrice;
    /** 委托数量 */
    private BigDecimal entrustQuantity;
    /** 已成交数量 */
    private BigDecimal filledQuantity;
    /** 平均成交价 */
    private BigDecimal avgFilledPrice;
    /** 已实现盈亏 */
    private BigDecimal realizedPnl;
    /** 订单状态 */
    private String status;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
