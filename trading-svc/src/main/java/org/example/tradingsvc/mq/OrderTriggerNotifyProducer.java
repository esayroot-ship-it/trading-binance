package org.example.tradingsvc.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.mq.MqConstants;
import org.example.common.mq.OrderTriggerNotifyMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTriggerNotifyProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void send(OrderTriggerNotifyMessage message) {
        if (message == null || message.getUserId() == null || message.getUserId() <= 0) {
            return;
        }
        try {
            rabbitTemplate.convertAndSend(
                    MqConstants.TRADING_ORDER_TRIGGER_NOTIFY_QUEUE,
                    objectMapper.writeValueAsString(message));
        } catch (Exception ex) {
            log.error("发送订单触发通知消息失败，订单号={}", message.getOrderNo(), ex);
        }
    }
}
