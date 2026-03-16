package org.example.messagesvc.config;

import org.example.common.mq.MqConstants;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public Queue socialCommentNotifyQueue() {
        return QueueBuilder.durable(MqConstants.SOCIAL_COMMENT_NOTIFY_QUEUE).build();
    }

    @Bean
    public Queue tradingOrderTriggerNotifyQueue() {
        return QueueBuilder.durable(MqConstants.TRADING_ORDER_TRIGGER_NOTIFY_QUEUE).build();
    }
}
