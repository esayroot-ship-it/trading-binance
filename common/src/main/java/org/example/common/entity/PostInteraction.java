package org.example.common.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 帖子互动实体，对应 post_interaction 表。
 */
@Data
public class PostInteraction {
    /** 主键ID */
    private Long id;
    /** 帖子编号 */
    private Long postId;
    /** 用户编号 */
    private Long userId;
    /** 行为类型：1点赞，2收藏 */
    private Integer actionType;
    /** 创建时间 */
    private LocalDateTime createTime;
}
