package org.example.usersvc.service;

import org.example.common.DTO.TradingPlatformDTO;
import tools.R;

public interface UserProfileService {

    R<TradingPlatformDTO.UserInfoResponse> updatePassword(Long userId, TradingPlatformDTO.UserUpdatePasswordRequest request);

    R<TradingPlatformDTO.UserInfoResponse> updateNickname(Long userId, TradingPlatformDTO.UserUpdateNicknameRequest request);

    R<TradingPlatformDTO.UserInfoResponse> updateEmail(Long userId, TradingPlatformDTO.UserUpdateEmailRequest request);

    R<TradingPlatformDTO.UserInfoResponse> updateAvatar(Long userId, TradingPlatformDTO.UserUpdateAvatarRequest request);

    R<Void> cancelAccount(Long userId, TradingPlatformDTO.UserCancelAccountRequest request);
}
