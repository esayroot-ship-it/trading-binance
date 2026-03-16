package org.example.tradingsvc.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.mq.MarketPriceChangeMessage;
import org.example.common.mq.MqConstants;
import org.example.tradingsvc.service.TradingOrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketPriceChangeConsumer {

    private final ObjectMapper objectMapper;
    private final TradingOrderService tradingOrderService;

    @RabbitListener(queues = MqConstants.MARKET_PRICE_CHANGE_QUEUE)
    public void consume(String payload) {
        try {
            MarketPriceChangeMessage message = objectMapper.readValue(payload, MarketPriceChangeMessage.class);
            if (message == null || !StringUtils.hasText(message.getSymbol()) || message.getPrice() == null) {
                return;
            }
            log.debug("消费市场价格变动事件，标的={}，价格={}，更新时间={}",
                    message.getSymbol(),
                    message.getPrice(),
                    message.getUpdateTime());
            tradingOrderService.onPriceChanged(message.getSymbol(), message.getPrice(), message.getUpdateTime());
        } catch (Exception ex) {
            log.error("消费市场价格变动事件失败，消息体={}", payload, ex);
            throw new RuntimeException(ex);
        }
    }
}
