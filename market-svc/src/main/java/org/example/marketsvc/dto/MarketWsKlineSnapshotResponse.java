package org.example.marketsvc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 前端行情快照响应（Binance WebSocket 风格）。
 */
@Data
public class MarketWsKlineSnapshotResponse {

    /**
     * 流名称，例如 btcusdt@kline_1m。
     */
    private String stream;

    /**
     * Binance 风格事件包装对象。
     */
    private WsKlineData data;

    /**
     * Redis 缓存中的K线原始JSON数组。
     */
    private String klineJson;

    /**
     * 当前展示价格（通常取最新收盘价）。
     */
    private BigDecimal currentPrice;

    /**
     * 最近两根K线收盘价差额（最新收盘价 - 上一根收盘价）。
     */
    private BigDecimal twoKlineChangeAmount;

    /**
     * 最近两根K线涨跌幅百分比。
     */
    private BigDecimal twoKlineChangePercent;

    /**
     * 是否上涨（涨跌额 >= 0）。
     */
    private Boolean rising;

    /**
     * 快照更新时间戳（毫秒）。
     */
    private Long updateTime;

    @Data
    public static class WsKlineData {

        /**
         * 事件类型，固定为 kline。
         */
        @JsonProperty("e")
        private String e;

        /**
         * 事件时间戳（毫秒）。
         */
        @JsonProperty("E")
        private Long eventTime;

        /**
         * 交易标的代码。
         */
        @JsonProperty("s")
        private String s;

        /**
         * K线事件主体。
         */
        @JsonProperty("k")
        private WsKlinePayload k;
    }

    @Data
    public static class WsKlinePayload {

        /** K线开盘时间戳（毫秒）。 */
        @JsonProperty("t")
        private Long openTime;

        /** K线收盘时间戳（毫秒）。 */
        @JsonProperty("T")
        private Long closeTime;

        /** 交易标的代码。 */
        @JsonProperty("s")
        private String symbol;

        /** K线周期。 */
        @JsonProperty("i")
        private String interval;

        /** 开盘价。 */
        @JsonProperty("o")
        private String open;

        /** 收盘价。 */
        @JsonProperty("c")
        private String close;

        /** 最高价。 */
        @JsonProperty("h")
        private String high;

        /** 最低价。 */
        @JsonProperty("l")
        private String low;

        /** 成交量（基础资产）。 */
        @JsonProperty("v")
        private String volume;

        /** 成交笔数。 */
        @JsonProperty("n")
        private Integer tradeCount;

        /**
         * 该K线是否已经收盘。
         */
        @JsonProperty("x")
        private Boolean closed;

        /** 成交额（计价资产）。 */
        @JsonProperty("q")
        private String quoteVolume;

        /** 主动买入成交量（基础资产）。 */
        @JsonProperty("V")
        private String takerBuyBaseVolume;

        /** 主动买入成交额（计价资产）。 */
        @JsonProperty("Q")
        private String takerBuyQuoteVolume;

        /** 预留字段。 */
        @JsonProperty("B")
        private String ignore;
    }
}
