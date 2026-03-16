package org.example.socialsvc.dto;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 新闻与帖子统一列表项。
 */
@Data
public class ContentListItem {

    /** 内容ID。 */
    private Long id;

    /** 内容类型：NEWS 或 POST。 */
    private String contentType;

    /** 分类ID。 */
    private Long categoryId;

    /** 发布用户编号（新闻为空）。 */
    private Long userId;

    /** 标题。 */
    private String title;

    /** 摘要（新闻使用）。 */
    private String summary;

    /** 正文内容。 */
    private String content;

    /** 封面图URL（新闻使用）。 */
    private String imageUrl;

    /** 图片JSON（帖子使用）。 */
    private String images;

    /** 内容来源（新闻使用）。 */
    private String source;

    /** 浏览数（帖子使用）。 */
    private Integer viewCount;

    /** 点赞数（帖子使用）。 */
    private Integer likeCount;

    /** 评论数（帖子使用）。 */
    private Integer commentCount;

    /** 发布时间（新闻使用）。 */
    private LocalDateTime publishTime;

    /** 创建时间。 */
    private LocalDateTime createTime;
}
