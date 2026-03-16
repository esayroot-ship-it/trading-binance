package org.example.common.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 金融新闻实体，对应 news_article 表。
 */
@Data
public class NewsArticle {
    /** 主键ID */
    private Long id;
    /** 分类ID */
    private Long categoryId;
    /** 新闻标题 */
    private String title;
    /** 原摘要 */
    private String summary;
    /** AI摘要 */
    private String aiSummary;
    /** AI情绪 */
    private String aiSentiment;
    /** AI处理状态 */
    private Integer aiStatus;
    /** 正文内容 */
    private String content;
    /** 新闻来源 */
    private String source;
    /** 封面图URL */
    private String imageUrl;
    /** 状态：0隐藏，1正常 */
    private Integer status;
    /** 发布时间 */
    private LocalDateTime publishTime;
    /** 入库时间 */
    private LocalDateTime createTime;
}
