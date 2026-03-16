package org.example.marketsvc.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.mq.MarketPriceChangeMessage;
import org.example.common.mq.MqConstants;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketPriceChangeProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void sendPriceChange(String symbol, BigDecimal price, Long updateTime) {
        if (!StringUtils.hasText(symbol) || price == null) {
            return;
        }
        MarketPriceChangeMessage message = new MarketPriceChangeMessage();
        message.setSymbol(symbol.trim().toUpperCase(Locale.ROOT));
        message.setPrice(price);
        message.setUpdateTime(updateTime == null ? System.currentTimeMillis() : updateTime);
        try {
            rabbitTemplate.convertAndSend(
                    MqConstants.MARKET_PRICE_CHANGE_QUEUE,
                    objectMapper.writeValueAsString(message));
            log.debug("发布市场价格变动消息成功，标的={}，价格={}，更新时间={}",
                    message.getSymbol(),
                    message.getPrice(),
                    message.getUpdateTime());
        } catch (Exception ex) {
            log.error("发送市场价格变动消息失败，标的={}", symbol, ex);
        }
    }
}
