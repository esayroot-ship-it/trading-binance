package org.example.marketsvc.service;

import java.util.List;
import org.example.common.entity.AssetInfo;
import org.example.marketsvc.dto.MarketKlineResponse;
import org.example.marketsvc.dto.MarketQuoteResponse;
import org.example.marketsvc.dto.MarketWsKlineSnapshotResponse;

public interface MarketDataService {

    void warmupMarketCache();

    void refreshMarketCache();

    void refreshKlineCacheByRest();

    void reloadMarketCacheAndStream();

    List<AssetInfo> listActiveAssets();

    MarketQuoteResponse getQuote(String symbol);

    MarketKlineResponse getKline(String symbol, String timeframe);

    MarketWsKlineSnapshotResponse getFrontendPriceSnapshot(String symbol, String timeframe);
}
