package org.example.usersvc.service;

import org.example.common.DTO.TradingPlatformDTO;
import tools.R;

public interface AdminUserManageService {

    R<TradingPlatformDTO.UserPageResponse> pageUsers(Integer pageNo, Integer pageSize);

    R<TradingPlatformDTO.UserInfoResponse> getByUsername(String username);

    R<TradingPlatformDTO.UserInfoResponse> updateUserStatus(TradingPlatformDTO.AdminUpdateUserStatusRequest request);

    R<Void> deleteUser(TradingPlatformDTO.AdminDeleteUserRequest request);
}
