package org.example.tradingsvc.config;

import org.example.common.mq.MqConstants;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public Queue marketPriceChangeQueue() {
        return QueueBuilder.durable(MqConstants.MARKET_PRICE_CHANGE_QUEUE).build();
    }

    @Bean
    public Queue tradingOrderTriggerNotifyQueue() {
        return QueueBuilder.durable(MqConstants.TRADING_ORDER_TRIGGER_NOTIFY_QUEUE).build();
    }
}
