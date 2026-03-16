package org.example.socialsvc.dto;

import lombok.Data;

/**
 * 帖子评论创建请求。
 */
@Data
public class PostCommentCreateRequest {

    /** 父评论ID，空或0表示一级评论。 */
    private Long parentId;

    /** 评论内容。 */
    private String content;
}
