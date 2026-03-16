package org.example.socialsvc.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.mq.MqConstants;
import org.example.common.mq.PostCommentNotifyMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialCommentNotifyProducer {

    private static final int COMMENT_PREVIEW_MAX_LEN = 120;

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void sendPostCommentNotify(Long postId, Long postAuthorId, Long commentUserId, String commentContent) {
        if (postAuthorId == null || postAuthorId <= 0) {
            return;
        }

        PostCommentNotifyMessage message = new PostCommentNotifyMessage();
        message.setTargetUserId(postAuthorId);
        message.setPostId(postId);
        message.setPostAuthorId(postAuthorId);
        message.setCommentUserId(commentUserId);
        message.setMsgType("COMMENT");
        message.setTitle("New comment on your post");
        message.setContent(buildContent(postId, commentUserId, commentContent));
        message.setCreateTime(LocalDateTime.now());

        try {
            String payload = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(MqConstants.SOCIAL_COMMENT_NOTIFY_QUEUE, payload);
        } catch (JsonProcessingException ex) {
            log.error("序列化评论通知消息失败，帖子编号={}，作者编号={}", postId, postAuthorId, ex);
        } catch (Exception ex) {
            log.error("发送评论通知消息失败，帖子编号={}，作者编号={}", postId, postAuthorId, ex);
        }
    }

    private String buildContent(Long postId, Long commentUserId, String commentContent) {
        String preview;
        if (!StringUtils.hasText(commentContent)) {
            preview = "";
        } else {
            preview = commentContent.trim();
            if (preview.length() > COMMENT_PREVIEW_MAX_LEN) {
                preview = preview.substring(0, COMMENT_PREVIEW_MAX_LEN) + "...";
            }
        }
        return "User " + commentUserId + " commented on your post (" + postId + "): " + preview;
    }
}
