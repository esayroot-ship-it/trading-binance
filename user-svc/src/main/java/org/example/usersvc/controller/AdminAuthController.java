package org.example.usersvc.controller;

import lombok.RequiredArgsConstructor;
import org.example.common.DTO.TradingPlatformDTO;
import org.example.usersvc.service.AdminAuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.R;

/**
 * 管理员认证接口。
 */
@RestController
@RequestMapping("/api/users/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    /**
     * 管理员注册接口。
     */
    @PostMapping("/register")
    public R<TradingPlatformDTO.AdminLoginResponse> register(
            @RequestBody TradingPlatformDTO.AdminRegisterRequest request
    ) {
        return adminAuthService.register(request);
    }

    /**
     * 管理员登录接口。
     */
    @PostMapping("/login")
    public R<TradingPlatformDTO.AdminLoginResponse> login(
            @RequestBody TradingPlatformDTO.AdminLoginRequest request
    ) {
        return adminAuthService.login(request);
    }
}