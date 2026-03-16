package org.example.usersvc.service.impl;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.example.common.DTO.TradingPlatformDTO;
import org.example.common.entity.User;
import org.example.usersvc.cache.UserRedisCache;
import org.example.usersvc.mapper.UserMapper;
import org.example.usersvc.service.UserProfileService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.R;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserMapper userMapper;
    private final UserRedisCache userRedisCache;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<TradingPlatformDTO.UserInfoResponse> updatePassword(
            Long userId,
            TradingPlatformDTO.UserUpdatePasswordRequest request
    ) {
        if (userId == null || request == null
                || !StringUtils.hasText(request.getOldPassword())
                || !StringUtils.hasText(request.getNewPassword())) {
            return R.fail("userId, oldPassword and newPassword are required");
        }

        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted())) {
            return R.fail("user not found");
        }
        if (!passwordMatches(request.getOldPassword(), user.getPassword())) {
            return R.fail("old password is incorrect");
        }

        String newPassword = request.getNewPassword().trim();
        if (!StringUtils.hasText(newPassword)) {
            return R.fail("newPassword is required");
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = userMapper.updatePassword(user.getId(), passwordEncoder.encode(newPassword), now);
        if (updated <= 0) {
            return R.fail("update password failed");
        }

        return R.ok("update password success", reloadAndCacheUser(user.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<TradingPlatformDTO.UserInfoResponse> updateNickname(
            Long userId,
            TradingPlatformDTO.UserUpdateNicknameRequest request
    ) {
        if (userId == null || request == null || !StringUtils.hasText(request.getNickname())) {
            return R.fail("userId and nickname are required");
        }

        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted())) {
            return R.fail("user not found");
        }

        int updated = userMapper.updateNickname(user.getId(), request.getNickname().trim(), LocalDateTime.now());
        if (updated <= 0) {
            return R.fail("update nickname failed");
        }

        return R.ok("update nickname success", reloadAndCacheUser(user.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<TradingPlatformDTO.UserInfoResponse> updateEmail(
            Long userId,
            TradingPlatformDTO.UserUpdateEmailRequest request
    ) {
        if (userId == null || request == null || !StringUtils.hasText(request.getEmail())) {
            return R.fail("userId and email are required");
        }

        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted())) {
            return R.fail("user not found");
        }

        int updated = userMapper.updateEmail(user.getId(), request.getEmail().trim(), LocalDateTime.now());
        if (updated <= 0) {
            return R.fail("update email failed");
        }

        return R.ok("update email success", reloadAndCacheUser(user.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<TradingPlatformDTO.UserInfoResponse> updateAvatar(
            Long userId,
            TradingPlatformDTO.UserUpdateAvatarRequest request
    ) {
        if (userId == null || request == null || !StringUtils.hasText(request.getAvatarUrl())) {
            return R.fail("userId and avatarUrl are required");
        }

        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted())) {
            return R.fail("user not found");
        }

        int updated = userMapper.updateAvatar(user.getId(), request.getAvatarUrl().trim(), LocalDateTime.now());
        if (updated <= 0) {
            return R.fail("update avatar failed");
        }

        return R.ok("update avatar success", reloadAndCacheUser(user.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> cancelAccount(Long userId, TradingPlatformDTO.UserCancelAccountRequest request) {
        if (userId == null || request == null || !StringUtils.hasText(request.getPassword())) {
            return R.fail("userId and password are required");
        }

        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted())) {
            return R.fail("user not found");
        }

        if (!passwordMatches(request.getPassword(), user.getPassword())) {
            return R.fail("password is incorrect");
        }

        int updated = userMapper.updateDeleted(user.getId(), 1, 0, LocalDateTime.now());
        if (updated <= 0) {
            return R.fail("cancel account failed");
        }

        userRedisCache.removeUser(user.getUsername());
        return R.ok("cancel account success");
    }

    private boolean passwordMatches(String rawPassword, String encodedPassword) {
        if (!StringUtils.hasText(encodedPassword)) {
            return false;
        }
        if (isBcryptHash(encodedPassword)) {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        }
        return rawPassword.equals(encodedPassword);
    }

    private boolean isBcryptHash(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }

    private TradingPlatformDTO.UserInfoResponse reloadAndCacheUser(Long userId) {
        User updated = userMapper.selectById(userId);
        if (updated != null) {
            userRedisCache.putUser(updated);
            return toUserInfoResponse(updated);
        }
        return null;
    }

    private TradingPlatformDTO.UserInfoResponse toUserInfoResponse(User user) {
        TradingPlatformDTO.UserInfoResponse response = new TradingPlatformDTO.UserInfoResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setEmail(user.getEmail());
        response.setStatus(user.getStatus());
        response.setIsDeleted(user.getIsDeleted());
        response.setCreateTime(user.getCreateTime());
        response.setUpdateTime(user.getUpdateTime());
        return response;
    }
}
