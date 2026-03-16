package org.example.marketsvc.config;

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
}
