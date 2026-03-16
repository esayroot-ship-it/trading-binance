package org.example.usersvc.service;

import java.util.List;
import org.example.common.DTO.TradingPlatformDTO;
import tools.R;

public interface UserFollowService {

    R<List<TradingPlatformDTO.UserFollowItem>> listFollowees(Long userId);

    R<Void> follow(Long userId, Long followeeId);

    R<Void> unfollow(Long userId, Long followeeId);
}
