package org.example.usersvc.controller;

import lombok.RequiredArgsConstructor;
import org.example.common.DTO.TradingPlatformDTO;
import org.example.usersvc.service.UserAuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.R;

/**
 * 用户认证接口。
 */
@RestController
@RequestMapping("/api/users/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;

    /**
     * 用户注册接口。
     */
    @PostMapping("/register")
    public R<TradingPlatformDTO.LoginResponse> register(@RequestBody TradingPlatformDTO.UserRegisterRequest request) {
        return userAuthService.register(request);
    }

    /**
     * 用户登录接口。
     */
    @PostMapping("/login")
    public R<TradingPlatformDTO.LoginResponse> login(@RequestBody TradingPlatformDTO.UserLoginRequest request) {
        return userAuthService.login(request);
    }
}
