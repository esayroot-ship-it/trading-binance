package org.example.marketsvc.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.entity.AssetInfo;
import org.example.marketsvc.dto.AssetCreateRequest;
import org.example.marketsvc.mapper.AssetInfoMapper;
import org.example.marketsvc.service.MarketAssetAdminService;
import org.example.marketsvc.service.MarketDataService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.R;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketAssetAdminServiceImpl implements MarketAssetAdminService {

    private static final String DEFAULT_ASSET_TYPE = "CRYPTO";
    private static final String DEFAULT_MARKET = "BINANCE";

    private final AssetInfoMapper assetInfoMapper;
    private final MarketDataService marketDataService;

    @Override
    public R<List<AssetInfo>> listAllAssets() {
        return R.ok("query success", assetInfoMapper.selectAll());
    }

    @Override
    public R<Void> createAsset(AssetCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getSymbol()) || !StringUtils.hasText(request.getName())) {
            return R.fail("symbol and name are required");
        }
        String symbol = request.getSymbol().trim().toUpperCase(Locale.ROOT);
        AssetInfo exists = assetInfoMapper.selectBySymbol(symbol);
        if (exists != null) {
            return R.fail("asset symbol already exists");
        }

        AssetInfo assetInfo = new AssetInfo();
        assetInfo.setSymbol(symbol);
        assetInfo.setName(request.getName().trim());
        assetInfo.setAssetType(normalizeAssetType(request.getAssetType()));
        assetInfo.setMarket(normalizeMarket(request.getMarket()));
        assetInfo.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        assetInfo.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        assetInfo.setCreateTime(LocalDateTime.now());
        assetInfo.setUpdateTime(LocalDateTime.now());

        int inserted = assetInfoMapper.insert(assetInfo);
        if (inserted <= 0) {
            return R.fail("create asset failed");
        }
        reloadMarketRuntime();
        return R.ok("create asset success");
    }

    @Override
    public R<Void> deleteAsset(Long assetId) {
        if (assetId == null || assetId <= 0) {
            return R.fail("invalid assetId");
        }
        int deleted = assetInfoMapper.deleteById(assetId);
        if (deleted <= 0) {
            return R.fail("asset not found");
        }
        reloadMarketRuntime();
        return R.ok("delete asset success");
    }

    @Override
    public R<Void> updateAssetStatus(Long assetId, Integer status) {
        if (assetId == null || assetId <= 0 || status == null || (status != 0 && status != 1)) {
            return R.fail("invalid params");
        }
        int updated = assetInfoMapper.updateStatusById(assetId, status);
        if (updated <= 0) {
            return R.fail("asset not found");
        }
        reloadMarketRuntime();
        return R.ok("update status success");
    }

    private String normalizeAssetType(String assetType) {
        if (!StringUtils.hasText(assetType)) {
            return DEFAULT_ASSET_TYPE;
        }
        return assetType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeMarket(String market) {
        if (!StringUtils.hasText(market)) {
            return DEFAULT_MARKET;
        }
        return market.trim().toUpperCase(Locale.ROOT);
    }

    private void reloadMarketRuntime() {
        try {
            marketDataService.reloadMarketCacheAndStream();
        } catch (Exception ex) {
            log.warn("资产变更后重载市场缓存与实时流失败", ex);
        }
    }
}
