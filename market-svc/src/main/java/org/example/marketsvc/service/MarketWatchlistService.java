package org.example.marketsvc.service;

import java.util.List;
import org.example.common.entity.UserWatchlist;
import tools.R;

public interface MarketWatchlistService {

    R<List<UserWatchlist>> listUserWatchlist(Long userId);

    R<Void> addWatchlist(Long userId, String symbol, Integer sortOrder);

    R<Void> removeWatchlist(Long userId, String symbol);
}
