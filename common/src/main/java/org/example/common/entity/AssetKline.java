package org.example.common.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * K线缓存实体，对应 asset_kline 表。
 */
@Data
public class AssetKline {
    /** 主键ID */
    private Long id;
    /** 资产代码 */
    private String symbol;
    /** K线周期 */
    private String timeframe;
    /** 标准化OHLCV JSON数据 */
    private String klineJson;
    /** 最后一条K线时间戳 */
    private Long lastKlineTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
