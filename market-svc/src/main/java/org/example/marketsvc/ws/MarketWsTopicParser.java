package org.example.marketsvc.ws;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class MarketWsTopicParser {

    private static final String QUOTE_PREFIX = "quote:";
    private static final String KLINE_PREFIX = "kline:";
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{2,30}$");
    private static final Pattern TIMEFRAME_PATTERN = Pattern.compile("^[0-9]{1,3}[mhdwM]$");

    private MarketWsTopicParser() {
    }

    public static ParsedTopic parse(String rawTopic) {
        if (!StringUtils.hasText(rawTopic)) {
            return null;
        }
        String topic = rawTopic.trim();
        if (topic.regionMatches(true, 0, QUOTE_PREFIX, 0, QUOTE_PREFIX.length())) {
            String symbol = normalizeSymbol(topic.substring(QUOTE_PREFIX.length()));
            if (symbol == null) {
                return null;
            }
            return new ParsedTopic(TopicType.QUOTE, symbol, null, QUOTE_PREFIX + symbol);
        }
        if (!topic.regionMatches(true, 0, KLINE_PREFIX, 0, KLINE_PREFIX.length())) {
            return null;
        }
        String content = topic.substring(KLINE_PREFIX.length());
        String[] parts = content.split(":");
        if (parts.length != 2) {
            return null;
        }
        String symbol = normalizeSymbol(parts[0]);
        String timeframe = normalizeTimeframe(parts[1]);
        if (symbol == null || timeframe == null) {
            return null;
        }
        return new ParsedTopic(TopicType.KLINE, symbol, timeframe, KLINE_PREFIX + symbol + ":" + timeframe);
    }

    private static String normalizeSymbol(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            return null;
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        if (!SYMBOL_PATTERN.matcher(normalized).matches()) {
            return null;
        }
        return normalized;
    }

    private static String normalizeTimeframe(String timeframe) {
        if (!StringUtils.hasText(timeframe)) {
            return null;
        }
        String normalized = timeframe.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        if ("15min".equals(lower) || "15m".equals(lower)) {
            return "15m";
        }
        if ("1h".equals(lower) || "60m".equals(lower) || "60min".equals(lower)) {
            return "1h";
        }
        if ("4h".equals(lower) || "240m".equals(lower) || "240min".equals(lower)) {
            return "4h";
        }
        if ("1d".equals(lower) || "1day".equals(lower) || "24h".equals(lower)) {
            return "1d";
        }
        if (lower.matches("^[0-9]{1,3}min$")) {
            normalized = lower.substring(0, lower.length() - 3) + "m";
        } else if (lower.matches("^[0-9]{1,3}[mhdw]$")) {
            normalized = lower;
        }
        if (!TIMEFRAME_PATTERN.matcher(normalized).matches()) {
            return null;
        }
        return normalized;
    }

    public enum TopicType {
        QUOTE,
        KLINE
    }

    public record ParsedTopic(TopicType type, String symbol, String timeframe, String normalizedTopic) {
    }
}
