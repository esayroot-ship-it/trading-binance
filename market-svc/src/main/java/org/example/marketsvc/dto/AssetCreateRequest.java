package org.example.marketsvc.dto;

import lombok.Data;

/**
 * 管理员创建交易标的请求。
 */
@Data
public class AssetCreateRequest {

    /** 标的代码，例如 BTCUSDT。 */
    private String symbol;

    /** 标的名称。 */
    private String name;

    /** 资产类型，例如 CRYPTO。 */
    private String assetType;

    /** 所属市场，例如 BINANCE。 */
    private String market;

    /** 上下架状态：1上架，0下架。 */
    private Integer status;

    /** 排序权重，值越大越靠前。 */
    private Integer sortOrder;
}
