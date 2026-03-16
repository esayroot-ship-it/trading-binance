package org.example.common.mq;

/**
 * MQ constants shared by services.
 */
public final class MqConstants {

    private MqConstants() {
    }

    /**
     * Queue for post-comment notification events.
     */
    public static final String SOCIAL_COMMENT_NOTIFY_QUEUE = "social.comment.notify.queue";

    /**
     * Queue for market price change events.
     */
    public static final String MARKET_PRICE_CHANGE_QUEUE = "market.price.change.queue";

    /**
     * Queue for notifying users when pending orders are triggered and filled.
     */
    public static final String TRADING_ORDER_TRIGGER_NOTIFY_QUEUE = "trading.order.trigger.notify.queue";
}
