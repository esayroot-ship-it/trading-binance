package org.example.marketsvc.controller;

import lombok.RequiredArgsConstructor;
import org.example.common.entity.UserWatchlist;
import org.example.marketsvc.service.MarketWatchlistService;
import org.springframework.web.bind.annotation.*;
import tools.R;

import java.util.List;

/**
 * 行情收藏接口。
 */
@RestController
@RequestMapping("/api/market/watchlist")
@RequiredArgsConstructor
public class MarketWatchlistController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final MarketWatchlistService marketWatchlistService;

    /**
     * 列出用户收藏的资产。
     */
    @GetMapping
    public R<List<UserWatchlist>> list(@RequestHeader(USER_ID_HEADER) Long userId) {
        return marketWatchlistService.listUserWatchlist(userId);
    }

    /**
     * 添加资产到收藏。
     */
    @PostMapping
    public R<Void> add(@RequestHeader(USER_ID_HEADER) Long userId,
                       @RequestParam String symbol,
                       @RequestParam(required = false) Integer sortOrder) {
        return marketWatchlistService.addWatchlist(userId, symbol, sortOrder);
    }

    /**
     * 删除收藏的资产。
     */
    @DeleteMapping("/{symbol}")
    public R<Void> remove(@RequestHeader(USER_ID_HEADER) Long userId,
                          @PathVariable String symbol) {
        return marketWatchlistService.removeWatchlist(userId, symbol);
    }
}
