package org.example.common.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 帖子评论实体，对应 post_comment 表。
 */
@Data
public class PostComment {
    /** 主键ID */
    private Long id;
    /** 帖子编号 */
    private Long postId;
    /** 评论用户编号 */
    private Long userId;
    /** 父评论ID，0代表顶级评论 */
    private Long parentId;
    /** 评论内容 */
    private String content;
    /** 审核状态 */
    private Integer status;
    /** 软删除标记 */
    private Integer isDeleted;
    /** 创建时间 */
    private LocalDateTime createTime;
}
