package org.example.usersvc.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.common.entity.User;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class UserRedisCache {

    public static final String USER_HASH_KEY = "user:svc:users";
    public static final String USER_LIST_KEY = "user:svc:user-list";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public User getByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        Object json = stringRedisTemplate.opsForHash().get(USER_HASH_KEY, username);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json.toString(), User.class);
        } catch (JsonProcessingException ex) {
            stringRedisTemplate.opsForHash().delete(USER_HASH_KEY, username);
            return null;
        }
    }

    public void putUser(User user) {
        if (user == null || !StringUtils.hasText(user.getUsername())) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(user);
            stringRedisTemplate.opsForHash().put(USER_HASH_KEY, user.getUsername(), json);
            upsertUsername(user.getUsername());
        } catch (JsonProcessingException ignored) {
        }
    }

    public void removeUser(String username) {
        if (!StringUtils.hasText(username)) {
            return;
        }
        stringRedisTemplate.opsForHash().delete(USER_HASH_KEY, username);
        List<String> usernames = readUserList();
        usernames.remove(username);
        writeUserList(usernames);
    }

    public void resetCache(List<User> users) {
        stringRedisTemplate.delete(USER_HASH_KEY);
        stringRedisTemplate.delete(USER_LIST_KEY);
        refreshAllUsers(users);
    }

    public void refreshAllUsers(List<User> users) {
        stringRedisTemplate.delete(USER_HASH_KEY);
        if (users == null || users.isEmpty()) {
            writeUserList(new ArrayList<>());
            return;
        }
        Map<String, String> userMap = new HashMap<>();
        List<String> usernames = new ArrayList<>();
        for (User user : users) {
            if (user != null && StringUtils.hasText(user.getUsername())) {
                try {
                    userMap.put(user.getUsername(), objectMapper.writeValueAsString(user));
                    usernames.add(user.getUsername());
                } catch (JsonProcessingException ignored) {
                }
            }
        }
        if (!userMap.isEmpty()) {
            stringRedisTemplate.opsForHash().putAll(USER_HASH_KEY, userMap);
        }
        writeUserList(usernames);
    }

    public void ensureUserList() {
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(USER_LIST_KEY))) {
            stringRedisTemplate.opsForValue().set(USER_LIST_KEY, "[]");
        }
    }

    private void upsertUsername(String username) {
        List<String> usernames = readUserList();
        if (!usernames.contains(username)) {
            usernames.add(username);
            writeUserList(usernames);
        }
    }

    private List<String> readUserList() {
        String listJson = stringRedisTemplate.opsForValue().get(USER_LIST_KEY);
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

    private void writeUserList(List<String> usernames) {
        try {
            stringRedisTemplate.opsForValue().set(USER_LIST_KEY, objectMapper.writeValueAsString(usernames));
        } catch (JsonProcessingException ex) {
            stringRedisTemplate.opsForValue().set(USER_LIST_KEY, "[]");
        }
    }
}
