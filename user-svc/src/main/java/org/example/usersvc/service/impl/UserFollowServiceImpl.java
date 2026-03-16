package org.example.usersvc.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.common.DTO.TradingPlatformDTO;
import org.example.common.entity.User;
import org.example.common.entity.UserFollow;
import org.example.usersvc.mapper.UserFollowMapper;
import org.example.usersvc.mapper.UserMapper;
import org.example.usersvc.service.UserFollowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.R;

@Service
@RequiredArgsConstructor
public class UserFollowServiceImpl implements UserFollowService {

    private final UserFollowMapper userFollowMapper;
    private final UserMapper userMapper;

    @Override
    public R<List<TradingPlatformDTO.UserFollowItem>> listFollowees(Long userId) {
        if (!isValidId(userId)) {
            return R.fail("userId is required");
        }

        User currentUser = userMapper.selectById(userId);
        if (!isActiveUser(currentUser)) {
            return R.fail("user not found");
        }

        List<TradingPlatformDTO.UserFollowItem> list = userFollowMapper.selectFolloweesByFollowerId(userId);
        if (list == null) {
            list = new ArrayList<>();
        }
        return R.ok("query success", list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> follow(Long userId, Long followeeId) {
        if (!isValidId(userId) || !isValidId(followeeId)) {
            return R.fail("userId and followeeId are required");
        }
        if (userId.equals(followeeId)) {
            return R.fail("cannot follow yourself");
        }

        User currentUser = userMapper.selectById(userId);
        if (!isActiveUser(currentUser)) {
            return R.fail("user not found");
        }

        User followee = userMapper.selectById(followeeId);
        if (!isActiveUser(followee)) {
            return R.fail("followee user not found");
        }

        UserFollow exists = userFollowMapper.selectByFollowerAndFollowee(userId, followeeId);
        if (exists != null) {
            return R.ok("already followed");
        }

        UserFollow relation = new UserFollow();
        relation.setFollowerId(userId);
        relation.setFolloweeId(followeeId);
        relation.setCreateTime(LocalDateTime.now());
        int inserted = userFollowMapper.insert(relation);
        if (inserted <= 0) {
            return R.fail("follow failed");
        }
        return R.ok("follow success");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> unfollow(Long userId, Long followeeId) {
        if (!isValidId(userId) || !isValidId(followeeId)) {
            return R.fail("userId and followeeId are required");
        }

        User currentUser = userMapper.selectById(userId);
        if (!isActiveUser(currentUser)) {
            return R.fail("user not found");
        }

        int deleted = userFollowMapper.deleteByFollowerAndFollowee(userId, followeeId);
        if (deleted <= 0) {
            return R.fail("follow relation not found");
        }
        return R.ok("unfollow success");
    }

    private boolean isValidId(Long id) {
        return id != null && id > 0;
    }

    private boolean isActiveUser(User user) {
        return user != null && !Integer.valueOf(1).equals(user.getIsDeleted());
    }
}
