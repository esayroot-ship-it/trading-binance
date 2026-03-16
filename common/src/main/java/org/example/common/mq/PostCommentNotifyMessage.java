package org.example.common.mq;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * Message payload for notifying post author about a new comment.
 */
@Data
public class PostCommentNotifyMessage {

    private Long targetUserId;

    private Long postId;

    private Long postAuthorId;

    private Long commentUserId;

    private String title;

    private String content;

    private String msgType;

    private LocalDateTime createTime;
}
