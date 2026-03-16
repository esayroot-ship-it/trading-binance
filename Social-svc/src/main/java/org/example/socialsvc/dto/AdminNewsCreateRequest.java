package org.example.socialsvc.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 管理员新增新闻请求。
 */
@Data
public class AdminNewsCreateRequest {

    /** 分类ID（NEWS模块分类）。 */
    private Long categoryId;

    /** 新闻标题。 */
    private String title;

    /** 新闻摘要。 */
    private String summary;

    /** 新闻正文。 */
    private String content;

    /** 新闻来源。 */
    private String source;

    /** 封面图URL。 */
    private String imageUrl;

    /** 发布时间，为空时默认当前时间。 */
    private LocalDateTime publishTime;

    /** 关联的金融标的代码列表。 */
    private List<String> symbols;
}
