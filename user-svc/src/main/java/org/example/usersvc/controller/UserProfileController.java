package org.example.usersvc.controller;

import lombok.RequiredArgsConstructor;
import org.example.common.DTO.TradingPlatformDTO;
import org.example.usersvc.service.UserProfileService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.R;

/**
 * 用户资料管理接口。
 * 提供密码、昵称、邮箱、头像 URL 的修改能力。
 */
@RestController
@RequestMapping("/api/users/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * 修改用户密码。
     */
    @PutMapping("/password")
    public R<TradingPlatformDTO.UserInfoResponse> updatePassword(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody TradingPlatformDTO.UserUpdatePasswordRequest request
    ) {
        return userProfileService.updatePassword(userId, request);
    }

    /**
     * 修改用户昵称。
     */
    @PutMapping("/nickname")
    public R<TradingPlatformDTO.UserInfoResponse> updateNickname(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody TradingPlatformDTO.UserUpdateNicknameRequest request
    ) {
        return userProfileService.updateNickname(userId, request);
    }

    /**
     * 修改用户邮箱。
     */
    @PutMapping("/email")
    public R<TradingPlatformDTO.UserInfoResponse> updateEmail(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody TradingPlatformDTO.UserUpdateEmailRequest request
    ) {
        return userProfileService.updateEmail(userId, request);
    }

    /**
     * 修改用户头像 URL。
     */
    @PutMapping("/avatar")
    public R<TradingPlatformDTO.UserInfoResponse> updateAvatar(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody TradingPlatformDTO.UserUpdateAvatarRequest request
    ) {
        return userProfileService.updateAvatar(userId, request);
    }

    /**
     * 用户注销自己的账户。
     */
    @DeleteMapping("/cancel")
    public R<Void> cancelAccount(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody TradingPlatformDTO.UserCancelAccountRequest request
    ) {
        return userProfileService.cancelAccount(userId, request);
    }
}
