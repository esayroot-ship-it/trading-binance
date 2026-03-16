package org.example.usersvc.startup;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.common.entity.User;
import org.example.usersvc.cache.UserRedisCache;
import org.example.usersvc.mapper.UserMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCacheWarmupRunner implements ApplicationRunner {

    private final UserMapper userMapper;
    private final UserRedisCache userRedisCache;

    @Override
    public void run(ApplicationArguments args) {
        List<User> users = userMapper.selectAll();
        userRedisCache.resetCache(users);
    }
}
