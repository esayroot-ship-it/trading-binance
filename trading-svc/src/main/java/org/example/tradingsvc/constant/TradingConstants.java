package org.example.tradingsvc.constant;

public final class TradingConstants {

    private TradingConstants() {
    }

    public static final String DEFAULT_CURRENCY = "USD";
    public static final String DEFAULT_ASSET_TYPE = "CRYPTO";

    public static final String ORDER_STATUS_PENDING = "PENDING";
    public static final String ORDER_STATUS_FILLED = "FILLED";
    public static final String ORDER_STATUS_CANCELED = "CANCELED";

    public static final String ORDER_TYPE_MARKET = "MARKET";
    public static final String ORDER_TYPE_LIMIT = "LIMIT";
    public static final String ORDER_TYPE_STOP_LOSS = "STOP_LOSS";
    public static final String ORDER_TYPE_TAKE_PROFIT = "TAKE_PROFIT";

    public static final String ACTION_OPEN = "OPEN";
    public static final String ACTION_CLOSE = "CLOSE";

    public static final String DIRECTION_LONG = "LONG";
    public static final String DIRECTION_SHORT = "SHORT";

    public static final String ACCOUNT_TARGET_BALANCE = "BALANCE";
    public static final String ACCOUNT_TARGET_FROZEN = "FROZEN";
    public static final String ACCOUNT_OP_INCREASE = "INCREASE";
    public static final String ACCOUNT_OP_DECREASE = "DECREASE";

    public static final Integer POSITION_STATUS_HOLDING = 1;
    public static final Integer POSITION_STATUS_CLOSED = 0;

    public static final String MARKET_PRICE_CACHE_PREFIX = "market:price:";
}
