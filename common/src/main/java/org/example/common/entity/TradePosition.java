package org.example.common.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 持仓实体，对应 trade_position 表。
 */
@Data
public class TradePosition {
    /** 主键ID */
    private Long id;
    /** 用户编号 */
    private Long userId;
    /** 资产代码 */
    private String symbol;
    /** 资产类型 */
    private String assetType;
    /** 持仓方向 */
    private String direction;
    /** 杠杆倍数 */
    private Integer leverage;
    /** 总持仓数量 */
    private BigDecimal holdQuantity;
    /** 可平仓数量 */
    private BigDecimal availableQuantity;
    /** 开仓均价 */
    private BigDecimal openPrice;
    /** 占用保证金 */
    private BigDecimal margin;
    /** 预估强平价 */
    private BigDecimal liquidationPrice;
    /** 状态：1持仓中，0已清仓 */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
