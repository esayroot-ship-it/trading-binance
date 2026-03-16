package org.example.usersvc.startup;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.common.entity.SysAdmin;
import org.example.usersvc.cache.AdminRedisCache;
import org.example.usersvc.mapper.SysAdminMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminCacheWarmupRunner implements ApplicationRunner {

    private final SysAdminMapper sysAdminMapper;
    private final AdminRedisCache adminRedisCache;

    @Override
    public void run(ApplicationArguments args) {
        List<SysAdmin> admins = sysAdminMapper.selectAll();
        adminRedisCache.resetCache(admins);
    }
}
