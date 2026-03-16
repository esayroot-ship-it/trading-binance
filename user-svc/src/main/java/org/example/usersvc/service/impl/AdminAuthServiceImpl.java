package org.example.usersvc.service.impl;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.example.common.DTO.TradingPlatformDTO;
import org.example.common.entity.SysAdmin;
import org.example.usersvc.cache.AdminRedisCache;
import org.example.usersvc.mapper.SysAdminMapper;
import org.example.usersvc.service.AdminAuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.R;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final SysAdminMapper sysAdminMapper;
    private final AdminRedisCache adminRedisCache;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<TradingPlatformDTO.AdminLoginResponse> register(TradingPlatformDTO.AdminRegisterRequest request) {
        SysAdmin registerAdmin = toAdminEntity(request);
        if (!StringUtils.hasText(registerAdmin.getUsername()) || !StringUtils.hasText(registerAdmin.getPassword())) {
            return R.fail("username and password are required");
        }

        String username = registerAdmin.getUsername().trim();
        String rawPassword = registerAdmin.getPassword().trim();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(rawPassword)) {
            return R.fail("username and password are required");
        }

        SysAdmin existing = sysAdminMapper.selectByUsername(username);
        if (existing != null) {
            return R.fail("admin username already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        registerAdmin.setUsername(username);
        registerAdmin.setPassword(passwordEncoder.encode(rawPassword));
        registerAdmin.setRoleType(defaultRoleType(registerAdmin.getRoleType()));
        registerAdmin.setStatus(1);
        registerAdmin.setCreateTime(now);
        registerAdmin.setUpdateTime(now);

        int inserted = sysAdminMapper.insert(registerAdmin);
        if (inserted <= 0) {
            return R.fail("admin register failed");
        }

        SysAdmin savedAdmin = sysAdminMapper.selectByUsername(username);
        if (savedAdmin == null || savedAdmin.getId() == null) {
            return R.fail("admin register failed");
        }

        adminRedisCache.putAdmin(savedAdmin);
        return R.ok("admin register success", buildResponse(savedAdmin));
    }

    @Override
    public R<TradingPlatformDTO.AdminLoginResponse> login(TradingPlatformDTO.AdminLoginRequest request) {
        SysAdmin loginAdmin = toAdminEntity(request);
        if (!StringUtils.hasText(loginAdmin.getUsername()) || !StringUtils.hasText(loginAdmin.getPassword())) {
            return R.fail("username and password are required");
        }

        String username = loginAdmin.getUsername().trim();
        String rawPassword = loginAdmin.getPassword().trim();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(rawPassword)) {
            return R.fail("username and password are required");
        }

        // Login reads from Redis first, then falls back to DB.
        SysAdmin admin = adminRedisCache.getByUsername(username);
        boolean loadedFromCache = admin != null;
        if (admin == null) {
            admin = sysAdminMapper.selectByUsername(username);
            if (admin != null) {
                adminRedisCache.putAdmin(admin);
            }
        }

        if (admin == null) {
            return R.fail("username or password is incorrect");
        }
        if (Integer.valueOf(0).equals(admin.getStatus())) {
            return R.fail("admin account is disabled");
        }

        if (!passwordMatches(rawPassword, admin)) {
            if (loadedFromCache) {
                SysAdmin latestAdmin = sysAdminMapper.selectByUsername(username);
                if (latestAdmin != null) {
                    if (Integer.valueOf(0).equals(latestAdmin.getStatus())) {
                        return R.fail("admin account is disabled");
                    }
                    if (passwordMatches(rawPassword, latestAdmin)) {
                        adminRedisCache.putAdmin(latestAdmin);
                        return R.ok("admin login success", buildResponse(latestAdmin));
                    }
                }
            }
            return R.fail("username or password is incorrect");
        }

        return R.ok("admin login success", buildResponse(admin));
    }

    private boolean passwordMatches(String rawPassword, SysAdmin admin) {
        String encoded = admin.getPassword();
        if (!StringUtils.hasText(encoded)) {
            return false;
        }

        if (isBcryptHash(encoded)) {
            return passwordEncoder.matches(rawPassword, encoded);
        }

        // Backward compatibility for legacy plain text passwords.
        if (!rawPassword.equals(encoded)) {
            return false;
        }

        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setUpdateTime(LocalDateTime.now());
        sysAdminMapper.updateById(admin);
        adminRedisCache.putAdmin(admin);
        return true;
    }

    private Integer defaultRoleType(Integer roleType) {
        if (roleType == null || roleType <= 0) {
            return 2;
        }
        return roleType;
    }

    private boolean isBcryptHash(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }

    private TradingPlatformDTO.AdminLoginResponse buildResponse(SysAdmin admin) {
        TradingPlatformDTO.AdminLoginResponse response = new TradingPlatformDTO.AdminLoginResponse();
        response.setAdminId(admin.getId());
        response.setUsername(admin.getUsername());
        response.setRoleType(admin.getRoleType());
        response.setStatus(admin.getStatus());
        return response;
    }

    private SysAdmin toAdminEntity(TradingPlatformDTO.AdminRegisterRequest request) {
        SysAdmin admin = new SysAdmin();
        if (request == null) {
            return admin;
        }
        admin.setUsername(request.getUsername());
        admin.setPassword(request.getPassword());
        admin.setRoleType(request.getRoleType());
        return admin;
    }

    private SysAdmin toAdminEntity(TradingPlatformDTO.AdminLoginRequest request) {
        SysAdmin admin = new SysAdmin();
        if (request == null) {
            return admin;
        }
        admin.setUsername(request.getUsername());
        admin.setPassword(request.getPassword());
        return admin;
    }
}
