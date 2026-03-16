package org.example.common.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户关注关系实体，对应 user_follow 表。
 */
@Data
public class UserFollow {
    /** 主键ID */
    private Long id;
    /** 关注者ID */
    private Long followerId;
    /** 被关注者ID */
    private Long followeeId;
    /** 创建时间 */
    private LocalDateTime createTime;
}
