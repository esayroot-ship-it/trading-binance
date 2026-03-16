package org.example.usersvc.service;

import org.example.common.DTO.TradingPlatformDTO;
import tools.R;

/**
 * 管理员认证服务接口。
 */
public interface AdminAuthService {

    /**
     * 管理员注册。
     */
    R<TradingPlatformDTO.AdminLoginResponse> register(TradingPlatformDTO.AdminRegisterRequest request);

    /**
     * 管理员登录。
     */
    R<TradingPlatformDTO.AdminLoginResponse> login(TradingPlatformDTO.AdminLoginRequest request);
}
