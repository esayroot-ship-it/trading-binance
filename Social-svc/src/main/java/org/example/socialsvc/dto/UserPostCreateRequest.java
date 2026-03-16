package org.example.socialsvc.dto;

import java.util.List;
import lombok.Data;

/**
 * 用户发布帖子请求。
 */
@Data
public class UserPostCreateRequest {

    /** 分类ID（POST模块分类）。 */
    private Long categoryId;

    /** 帖子标题，可为空。 */
    private String title;

    /** 帖子正文内容。 */
    private String content;

    /** 图片JSON数组字符串。 */
    private String images;

    /** 关联的金融标的代码列表。 */
    private List<String> symbols;
}
