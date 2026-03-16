package org.example.common.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 社区帖子实体，对应 post 表。
 */
@Data
public class Post {
    /** 主键ID */
    private Long id;
    /** 发帖用户编号 */
    private Long userId;
    /** 分类ID */
    private Long categoryId;
    /** 帖子标题 */
    private String title;
    /** 帖子正文 */
    private String content;
    /** 图片URL数组(JSON) */
    private String images;
    /** 浏览数 */
    private Integer viewCount;
    /** 点赞数 */
    private Integer likeCount;
    /** 评论数 */
    private Integer commentCount;
    /** 状态：0隐藏，1正常 */
    private Integer status;
    /** 软删除标记 */
    private Integer isDeleted;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
