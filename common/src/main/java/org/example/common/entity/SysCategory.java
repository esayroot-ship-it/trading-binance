package org.example.common.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 系统分类实体，对应 sys_category 表。
 */
@Data
public class SysCategory {
    /** 主键ID */
    private Long id;
    /** 分类名称 */
    private String categoryName;
    /** 模块类型 */
    private String moduleType;
    /** 排序权重 */
    private Integer sortOrder;
    /** 状态：1正常，0禁用 */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createTime;
}
