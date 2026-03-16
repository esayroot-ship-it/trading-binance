package org.example.messagesvc.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.entity.UserMessage;
import org.example.common.mq.MqConstants;
import org.example.common.mq.PostCommentNotifyMessage;
import org.example.messagesvc.mapper.UserMessageMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostCommentNotifyConsumer {

    private final ObjectMapper objectMapper;
    private final UserMessageMapper userMessageMapper;

    @RabbitListener(queues = MqConstants.SOCIAL_COMMENT_NOTIFY_QUEUE)
    public void consume(String payload) {
        try {
            PostCommentNotifyMessage message = objectMapper.readValue(payload, PostCommentNotifyMessage.class);
            if (message.getTargetUserId() == null || message.getTargetUserId() <= 0) {
                log.warn("忽略无效的评论通知消息: {}", payload);
                return;
            }
            UserMessage userMessage = new UserMessage();
            userMessage.setUserId(message.getTargetUserId());
            userMessage.setTitle(message.getTitle());
            userMessage.setContent(message.getContent());
            userMessage.setMsgType(message.getMsgType() == null ? "COMMENT" : message.getMsgType());
            userMessage.setIsRead(0);
            userMessage.setCreateTime(
                    message.getCreateTime() == null ? LocalDateTime.now() : message.getCreateTime());
            userMessageMapper.insert(userMessage);
        } catch (Exception ex) {
            log.error("消费评论通知消息失败，消息体={}", payload, ex);
            throw new RuntimeException(ex);
        }
    }
}
