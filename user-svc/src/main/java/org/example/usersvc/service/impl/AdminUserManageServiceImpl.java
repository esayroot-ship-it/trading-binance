package org.example.usersvc.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.common.DTO.TradingPlatformDTO;
import org.example.common.entity.SysAdmin;
import org.example.common.entity.User;
import org.example.usersvc.cache.UserRedisCache;
import org.example.usersvc.mapper.SysAdminMapper;
import org.example.usersvc.mapper.UserMapper;
import org.example.usersvc.service.AdminUserManageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.R;

@Service
@RequiredArgsConstructor
public class AdminUserManageServiceImpl implements AdminUserManageService {

    private final UserMapper userMapper;
    private final SysAdminMapper sysAdminMapper;
    private final UserRedisCache userRedisCache;

    @Override
    public R<TradingPlatformDTO.UserPageResponse> pageUsers(Integer pageNo, Integer pageSize) {
        int page = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
        int offset = (page - 1) * size;

        long total = userMapper.countAll();
        List<User> users = total > 0 ? userMapper.selectPage(offset, size) : new ArrayList<>();

        TradingPlatformDTO.UserPageResponse response = new TradingPlatformDTO.UserPageResponse();
        response.setTotal(total);
        response.setPageNo(page);
        response.setPageSize(size);
        response.setRecords(toUserInfoList(users));
        return R.ok("query success", response);
    }

    @Override
    public R<TradingPlatformDTO.UserInfoResponse> getByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return R.fail("username is required");
        }

        User user = userMapper.selectByUsername(username.trim());
        if (user == null) {
            return R.fail("user not found");
        }
        return R.ok("query success", toUserInfoResponse(user));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<TradingPlatformDTO.UserInfoResponse> updateUserStatus(TradingPlatformDTO.AdminUpdateUserStatusRequest request) {
        if (request == null || request.getAdminId() == null || request.getUserId() == null || request.getStatus() == null) {
            return R.fail("adminId, userId and status are required");
        }
        if (!Integer.valueOf(0).equals(request.getStatus()) && !Integer.valueOf(1).equals(request.getStatus())) {
            return R.fail("status must be 0 or 1");
        }

        SysAdmin admin = sysAdminMapper.selectById(request.getAdminId());
        if (admin == null || Integer.valueOf(0).equals(admin.getStatus())) {
            return R.fail("admin not available");
        }

        User user = userMapper.selectById(request.getUserId());
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted())) {
            return R.fail("user not found");
        }

        int updated = userMapper.updateStatus(user.getId(), request.getStatus(), LocalDateTime.now());
        if (updated <= 0) {
            return R.fail("update status failed");
        }

        User updatedUser = userMapper.selectById(user.getId());
        if (updatedUser != null) {
            userRedisCache.putUser(updatedUser);
        }
        return R.ok("update status success", toUserInfoResponse(updatedUser));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> deleteUser(TradingPlatformDTO.AdminDeleteUserRequest request) {
        if (request == null || request.getAdminId() == null || request.getUserId() == null) {
            return R.fail("adminId and userId are required");
        }

        SysAdmin admin = sysAdminMapper.selectById(request.getAdminId());
        if (admin == null || Integer.valueOf(0).equals(admin.getStatus())) {
            return R.fail("admin not available");
        }

        User user = userMapper.selectById(request.getUserId());
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted())) {
            return R.fail("user not found");
        }

        int updated = userMapper.updateDeleted(user.getId(), 1, 0, LocalDateTime.now());
        if (updated <= 0) {
            return R.fail("delete user failed");
        }

        userRedisCache.removeUser(user.getUsername());
        return R.ok("delete user success");
    }

    private List<TradingPlatformDTO.UserInfoResponse> toUserInfoList(List<User> users) {
        List<TradingPlatformDTO.UserInfoResponse> result = new ArrayList<>();
        if (users == null || users.isEmpty()) {
            return result;
        }
        for (User user : users) {
            result.add(toUserInfoResponse(user));
        }
        return result;
    }

    private TradingPlatformDTO.UserInfoResponse toUserInfoResponse(User user) {
        if (user == null) {
            return null;
        }
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
