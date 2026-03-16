package org.example.usersvc.service;

import org.example.common.DTO.TradingPlatformDTO;
import tools.R;

/**
 * 用户认证服务接口。
 */
public interface UserAuthService {

    /**
     * 用户注册。
     */
    R<TradingPlatformDTO.LoginResponse> register(TradingPlatformDTO.UserRegisterRequest request);

    /**
     * 用户登录。
     */
    R<TradingPlatformDTO.LoginResponse> login(TradingPlatformDTO.UserLoginRequest request);
}
