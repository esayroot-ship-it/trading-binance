package org.example.marketsvc.controller;

import lombok.RequiredArgsConstructor;
import org.example.common.entity.AssetInfo;
import org.example.marketsvc.dto.AssetCreateRequest;
import org.example.marketsvc.service.MarketAssetAdminService;
import org.springframework.web.bind.annotation.*;
import tools.R;

import java.util.List;

/**
 * 资产管理接口。
 */
@RestController
@RequestMapping("/api/market/admin/assets")
@RequiredArgsConstructor
public class MarketAdminAssetController {

    private final MarketAssetAdminService marketAssetAdminService;

    /**
     * 列出所有资产。
     */
    @GetMapping
    public R<List<AssetInfo>> listAllAssets() {
        return marketAssetAdminService.listAllAssets();
    }

    /**
     * 创建资产。
     */
    @PostMapping
    public R<Void> createAsset(@RequestBody AssetCreateRequest request) {
        return marketAssetAdminService.createAsset(request);
    }

    /**
     * 删除资产。
     */
    @DeleteMapping("/{assetId}")
    public R<Void> deleteAsset(@PathVariable Long assetId) {
        return marketAssetAdminService.deleteAsset(assetId);
    }

    /**
     * 更新资产状态。
     */
    @PutMapping("/{assetId}/status")
    public R<Void> updateStatus(@PathVariable Long assetId, @RequestParam Integer status) {
        return marketAssetAdminService.updateAssetStatus(assetId, status);
    }
}
