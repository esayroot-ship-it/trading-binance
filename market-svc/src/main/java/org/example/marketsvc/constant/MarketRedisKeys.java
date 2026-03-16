package org.example.marketsvc.constant;

public final class MarketRedisKeys {

    private static final String PRICE_PREFIX = "market:price:";
    private static final String KLINE_PREFIX = "market:kline:";

    private MarketRedisKeys() {
    }

    public static String priceKey(String symbol) {
        return PRICE_PREFIX + symbol;
    }

    public static String klineKey(String symbol, String timeframe) {
        return KLINE_PREFIX + symbol + ":" + timeframe;
    }

    public static String pricePattern() {
        return PRICE_PREFIX + "*";
    }

    public static String klinePattern() {
        return KLINE_PREFIX + "*";
    }
}
