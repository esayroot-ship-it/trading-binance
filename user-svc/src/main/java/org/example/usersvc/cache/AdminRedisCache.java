package org.example.usersvc.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.common.entity.SysAdmin;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AdminRedisCache {

    public static final String ADMIN_HASH_KEY = "user:svc:sys-admins";
    public static final String ADMIN_LIST_KEY = "user:svc:sys-admin-list";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public SysAdmin getByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        Object json = stringRedisTemplate.opsForHash().get(ADMIN_HASH_KEY, username);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json.toString(), SysAdmin.class);
        } catch (JsonProcessingException ex) {
            stringRedisTemplate.opsForHash().delete(ADMIN_HASH_KEY, username);
            return null;
        }
    }

    public void putAdmin(SysAdmin admin) {
        if (admin == null || !StringUtils.hasText(admin.getUsername())) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(admin);
            stringRedisTemplate.opsForHash().put(ADMIN_HASH_KEY, admin.getUsername(), json);
            upsertUsername(admin.getUsername());
        } catch (JsonProcessingException ignored) {
        }
    }

    public void resetCache(List<SysAdmin> admins) {
        stringRedisTemplate.delete(ADMIN_HASH_KEY);
        stringRedisTemplate.delete(ADMIN_LIST_KEY);
        refreshAllAdmins(admins);
    }

    public void refreshAllAdmins(List<SysAdmin> admins) {
        stringRedisTemplate.delete(ADMIN_HASH_KEY);
        if (admins == null || admins.isEmpty()) {
            writeAdminList(new ArrayList<>());
            return;
        }
        Map<String, String> adminMap = new HashMap<>();
        List<String> usernames = new ArrayList<>();
        for (SysAdmin admin : admins) {
            if (admin != null && StringUtils.hasText(admin.getUsername())) {
                try {
                    adminMap.put(admin.getUsername(), objectMapper.writeValueAsString(admin));
                    usernames.add(admin.getUsername());
                } catch (JsonProcessingException ignored) {
                }
            }
        }
        if (!adminMap.isEmpty()) {
            stringRedisTemplate.opsForHash().putAll(ADMIN_HASH_KEY, adminMap);
        }
        writeAdminList(usernames);
    }

    public void ensureAdminList() {
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(ADMIN_LIST_KEY))) {
            stringRedisTemplate.opsForValue().set(ADMIN_LIST_KEY, "[]");
        }
    }

    private void upsertUsername(String username) {
        List<String> usernames = readAdminList();
        if (!usernames.contains(username)) {
            usernames.add(username);
            writeAdminList(usernames);
        }
    }

    private List<String> readAdminList() {
        String listJson = stringRedisTemplate.opsForValue().get(ADMIN_LIST_KEY);
        if (!StringUtils.hasText(listJson)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(listJson, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ex) {
            return new ArrayList<>();
        }
    }

    private void writeAdminList(List<String> usernames) {
        try {
            stringRedisTemplate.opsForValue().set(ADMIN_LIST_KEY, objectMapper.writeValueAsString(usernames));
        } catch (JsonProcessingException ex) {
            stringRedisTemplate.opsForValue().set(ADMIN_LIST_KEY, "[]");
        }
    }
}
