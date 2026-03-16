package org.example.marketsvc.service;

import java.util.List;
import org.example.common.entity.AssetInfo;
import org.example.marketsvc.dto.AssetCreateRequest;
import tools.R;

public interface MarketAssetAdminService {

    R<List<AssetInfo>> listAllAssets();

    R<Void> createAsset(AssetCreateRequest request);

    R<Void> deleteAsset(Long assetId);

    R<Void> updateAssetStatus(Long assetId, Integer status);
}
