package org.example.usersvc.controller;

import lombok.RequiredArgsConstructor;
import org.example.common.DTO.TradingPlatformDTO;
import org.example.usersvc.service.AdminUserManageService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.R;

/**
 * 管理员用户管理接口。
 * 提供用户分页查询、用户名查询、账户状态修改能力。
 */
@RestController
@RequestMapping("/api/users/admin/users")
@RequiredArgsConstructor
public class AdminUserManageController {

    private final AdminUserManageService adminUserManageService;

    /**
     * 分页查询所有用户。
     */
    @GetMapping("/page")
    public R<TradingPlatformDTO.UserPageResponse> pageUsers(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return adminUserManageService.pageUsers(pageNo, pageSize);
    }

    /**
     * 根据用户名查询用户信息。
     */
    @GetMapping("/username")
    public R<TradingPlatformDTO.UserInfoResponse> getByUsername(@RequestParam String username) {
        return adminUserManageService.getByUsername(username);
    }

    /**
     * 修改用户账户状态（0-禁用，1-正常）。
     */
    @PutMapping("/status")
    public R<TradingPlatformDTO.UserInfoResponse> updateUserStatus(
            @RequestBody TradingPlatformDTO.AdminUpdateUserStatusRequest request
    ) {
        return adminUserManageService.updateUserStatus(request);
    }

    /**
     * 管理员删除用户（逻辑删除）。
     */
    @DeleteMapping
    public R<Void> deleteUser(@RequestBody TradingPlatformDTO.AdminDeleteUserRequest request) {
        return adminUserManageService.deleteUser(request);
    }
}
