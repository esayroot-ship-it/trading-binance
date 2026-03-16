package org.example.common.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 交易标的基础信息实体，对应 asset_info 表。
 */
@Data
public class AssetInfo {
    /** 主键ID */
    private Long id;
    /** 全局唯一资产代码 */
    private String symbol;
    /** 资产展示名称 */
    private String name;
    /** 资产类型 */
    private String assetType;
    /** 所属交易市场 */
    private String market;
    /** 状态：0下架，1正常 */
    private Integer status;
    /** 排序权重 */
    private Integer sortOrder;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
