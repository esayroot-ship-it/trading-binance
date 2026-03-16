package org.example.marketsvc.dto;

import lombok.Data;

/**
 * Binance ticker/price 接口响应。
 */
@Data
public class BinanceTickerPriceResponse {

    /** 交易对代码。 */
    private String symbol;

    /** 最新价格字符串。 */
    private String price;
}
