package org.example.marketsvc.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.example.common.entity.AssetInfo;
import org.example.common.entity.UserWatchlist;
import org.example.marketsvc.mapper.AssetInfoMapper;
import org.example.marketsvc.mapper.UserWatchlistMapper;
import org.example.marketsvc.service.MarketWatchlistService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.R;

@Service
@RequiredArgsConstructor
public class MarketWatchlistServiceImpl implements MarketWatchlistService {

    private static final int ENABLED_STATUS = 1;

    private final UserWatchlistMapper userWatchlistMapper;
    private final AssetInfoMapper assetInfoMapper;

    @Override
    public R<List<UserWatchlist>> listUserWatchlist(Long userId) {
        if (userId == null || userId <= 0) {
            return R.fail("invalid userId");
        }
        return R.ok("query success", userWatchlistMapper.selectByUserId(userId));
    }

    @Override
    public R<Void> addWatchlist(Long userId, String symbol, Integer sortOrder) {
        if (userId == null || userId <= 0) {
            return R.fail("invalid userId");
        }
        if (!StringUtils.hasText(symbol)) {
            return R.fail("symbol is required");
        }

        String safeSymbol = normalizeSymbol(symbol);
        AssetInfo assetInfo = assetInfoMapper.selectBySymbol(safeSymbol);
        if (assetInfo == null) {
            return R.fail("asset symbol not found");
        }
        if (assetInfo.getStatus() == null || assetInfo.getStatus() != ENABLED_STATUS) {
            return R.fail("asset is disabled");
        }

        UserWatchlist exists = userWatchlistMapper.selectByUserIdAndSymbol(userId, safeSymbol);
        if (exists != null) {
            return R.fail("symbol already in watchlist");
        }

        int finalSortOrder = resolveSortOrder(userId, sortOrder);
        UserWatchlist entity = new UserWatchlist();
        entity.setUserId(userId);
        entity.setSymbol(safeSymbol);
        entity.setSortOrder(finalSortOrder);
        entity.setCreateTime(LocalDateTime.now());
        int inserted = userWatchlistMapper.insert(entity);
        if (inserted <= 0) {
            return R.fail("add watchlist failed");
        }
        return R.ok("add watchlist success");
    }

    @Override
    public R<Void> removeWatchlist(Long userId, String symbol) {
        if (userId == null || userId <= 0) {
            return R.fail("invalid userId");
        }
        if (!StringUtils.hasText(symbol)) {
            return R.fail("symbol is required");
        }
        String safeSymbol = normalizeSymbol(symbol);
        int deleted = userWatchlistMapper.deleteByUserIdAndSymbol(userId, safeSymbol);
        if (deleted <= 0) {
            return R.fail("symbol not in watchlist");
        }
        return R.ok("remove watchlist success");
    }

    private int resolveSortOrder(Long userId, Integer sortOrder) {
        if (sortOrder != null) {
            return sortOrder;
        }
        Integer currentMax = userWatchlistMapper.selectMaxSortOrderByUserId(userId);
        if (currentMax == null) {
            return 1;
        }
        return currentMax + 1;
    }

    private String normalizeSymbol(String symbol) {
        return symbol.trim().toUpperCase(Locale.ROOT);
    }
}
