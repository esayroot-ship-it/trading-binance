package org.example.messagesvc.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.entity.UserMessage;
import org.example.common.mq.MqConstants;
import org.example.common.mq.OrderTriggerNotifyMessage;
import org.example.messagesvc.mapper.UserMessageMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTriggerNotifyConsumer {

    private static final String DEFAULT_MSG_TYPE = "ORDER_TRIGGER";

    private final ObjectMapper objectMapper;
    private final UserMessageMapper userMessageMapper;

    @RabbitListener(queues = MqConstants.TRADING_ORDER_TRIGGER_NOTIFY_QUEUE)
    public void consume(String payload) {
        try {
            OrderTriggerNotifyMessage message = objectMapper.readValue(payload, OrderTriggerNotifyMessage.class);
            if (message.getUserId() == null || message.getUserId() <= 0) {
                log.warn("忽略无效的订单触发通知消息: {}", payload);
                return;
            }

            UserMessage userMessage = new UserMessage();
            userMessage.setUserId(message.getUserId());
            userMessage.setTitle(buildTitle(message));
            userMessage.setContent(buildContent(message));
            userMessage.setMsgType(StringUtils.hasText(message.getMsgType()) ? message.getMsgType() : DEFAULT_MSG_TYPE);
            userMessage.setIsRead(0);
            userMessage.setCreateTime(message.getCreateTime() == null ? LocalDateTime.now() : message.getCreateTime());
            userMessageMapper.insert(userMessage);
        } catch (Exception ex) {
            log.error("消费订单触发通知消息失败，消息体={}", payload, ex);
            throw new RuntimeException(ex);
        }
    }

    private String buildTitle(OrderTriggerNotifyMessage message) {
        if (StringUtils.hasText(message.getTitle())) {
            return message.getTitle();
        }
        return "Order triggered and filled";
    }

    private String buildContent(OrderTriggerNotifyMessage message) {
        if (StringUtils.hasText(message.getContent())) {
            return message.getContent();
        }
        return "Order " + message.getOrderNo() + " triggered and filled. Symbol " + message.getSymbol();
    }
}
