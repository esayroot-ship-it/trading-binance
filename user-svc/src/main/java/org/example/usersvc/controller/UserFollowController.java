package org.example.usersvc.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.common.DTO.TradingPlatformDTO;
import org.example.usersvc.service.UserFollowService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.R;

/**
 * 用户关注接口。
 * 用户编号由网关解析JWT后通过请求头 X-User-Id 透传。
 */
@RestController
@RequestMapping("/api/users/follows")
@RequiredArgsConstructor
public class UserFollowController {

    private final UserFollowService userFollowService;

    /**
     * 获取当前用户关注列表。
     */
    @GetMapping
    public R<List<TradingPlatformDTO.UserFollowItem>> listFollowees(@RequestHeader("X-User-Id") Long userId) {
        return userFollowService.listFollowees(userId);
    }

    /**
     * 关注指定用户。
     */
    @PostMapping("/{followeeId}")
    public R<Void> follow(@RequestHeader("X-User-Id") Long userId, @PathVariable Long followeeId) {
        return userFollowService.follow(userId, followeeId);
    }

    /**
     * 取消关注指定用户。
     */
    @DeleteMapping("/{followeeId}")
    public R<Void> unfollow(@RequestHeader("X-User-Id") Long userId, @PathVariable Long followeeId) {
        return userFollowService.unfollow(userId, followeeId);
    }
}
