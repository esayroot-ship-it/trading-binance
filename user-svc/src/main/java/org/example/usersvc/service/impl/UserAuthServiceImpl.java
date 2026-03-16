package org.example.usersvc.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.common.DTO.TradingPlatformDTO;
import org.example.common.entity.User;
import org.example.usersvc.cache.UserRedisCache;
import org.example.usersvc.mapper.UserMapper;
import org.example.usersvc.security.JwtTokenProvider;
import org.example.usersvc.service.UserAuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.R;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private final UserMapper userMapper;
    private final UserRedisCache userRedisCache;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<TradingPlatformDTO.LoginResponse> register(TradingPlatformDTO.UserRegisterRequest request) {
        User registerUser = toUserEntity(request);
        if (!StringUtils.hasText(registerUser.getUsername()) || !StringUtils.hasText(registerUser.getPassword())) {
            return R.fail("username and password are required");
        }

        String username = registerUser.getUsername().trim();
        String rawPassword = registerUser.getPassword().trim();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(rawPassword)) {
            return R.fail("username and password are required");
        }

        User existing = userMapper.selectByUsername(username);
        if (existing != null) {
            return R.fail("username already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        registerUser.setUsername(username);
        registerUser.setPassword(passwordEncoder.encode(rawPassword));
        registerUser.setEmail(normalizeOptional(registerUser.getEmail()));
        registerUser.setNickname(normalizeOptional(registerUser.getNickname()));
        registerUser.setStatus(1);
        registerUser.setIsDeleted(0);
        registerUser.setCreateTime(now);
        registerUser.setUpdateTime(now);

        int inserted = userMapper.insert(registerUser);
        if (inserted <= 0) {
            return R.fail("register failed");
        }

        User savedUser = userMapper.selectByUsername(username);
        if (savedUser == null || savedUser.getId() == null) {
            return R.fail("register failed");
        }

        userRedisCache.putUser(savedUser);
        return R.ok("register success", buildLoginResponse(savedUser));
    }

    @Override
    public R<TradingPlatformDTO.LoginResponse> login(TradingPlatformDTO.UserLoginRequest request) {
        User loginUser = toUserEntity(request);
        if (!StringUtils.hasText(loginUser.getUsername()) || !StringUtils.hasText(loginUser.getPassword())) {
            return R.fail("username and password are required");
        }

        String username = loginUser.getUsername().trim();
        String rawPassword = loginUser.getPassword();

        // Login reads from Redis first, then falls back to DB.
        User user = userRedisCache.getByUsername(username);
        if (user == null) {
            user = userMapper.selectByUsername(username);
            if (user != null) {
                userRedisCache.putUser(user);
            }
        }

        if (user == null) {
            return R.fail("username or password is incorrect");
        }
        if (Integer.valueOf(1).equals(user.getIsDeleted())) {
            return R.fail("user not found");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            return R.fail("account is disabled");
        }

        if (!passwordMatches(rawPassword, user)) {
            return R.fail("username or password is incorrect");
        }

        return R.ok("login success", buildLoginResponse(user));
    }

    private boolean passwordMatches(String rawPassword, User user) {
        String encoded = user.getPassword();
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

        String newPassword = passwordEncoder.encode(rawPassword);
        user.setPassword(newPassword);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        userRedisCache.putUser(user);
        return true;
    }

    private boolean isBcryptHash(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private TradingPlatformDTO.LoginResponse buildLoginResponse(User user) {
        JwtTokenProvider.TokenInfo tokenInfo = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        TradingPlatformDTO.LoginResponse response = new TradingPlatformDTO.LoginResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setToken(tokenInfo.token());
        response.setExpireAt(tokenInfo.expireAt());
        return response;
    }

    private User toUserEntity(TradingPlatformDTO.UserRegisterRequest request) {
        User user = new User();
        if (request == null) {
            return user;
        }
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname());
        return user;
    }

    private User toUserEntity(TradingPlatformDTO.UserLoginRequest request) {
        User user = new User();
        if (request == null) {
            return user;
        }
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        return user;
    }
}
