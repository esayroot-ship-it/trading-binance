package org.example.common.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户自选实体，对应 user_watchlist 表。
 */
@Data
public class UserWatchlist {
    /** 主键ID */
    private Long id;
    /** 用户编号 */
    private Long userId;
    /** 资产代码 */
    private String symbol;
    /** 自定义排序 */
    private Integer sortOrder;
    /** 创建时间 */
    private LocalDateTime createTime;
}
