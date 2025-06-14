package com.example.CollaborationService.SessionServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Set;

public class UserSessionService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String ACTIVE_USERS_KEY = "doc:users:";
    private static final String USER_SESSION_KEY = "user:session:";

    public void addUserToDocument(String documentId, String userId,String sessionId) {
        redisTemplate.opsForSet().add(ACTIVE_USERS_KEY + documentId, userId);
        redisTemplate.expire(ACTIVE_USERS_KEY + documentId, Duration.ofHours(24));
        redisTemplate.opsForValue()
                .set(USER_SESSION_KEY + sessionId, documentId + "+" + userId, Duration.ofHours(24));

    }

    public void removeUserFromDocument(String documentId, String userId, String sessionId) {
        redisTemplate.opsForSet().remove(ACTIVE_USERS_KEY + documentId, userId);
        redisTemplate.delete(USER_SESSION_KEY + sessionId);
    }

    public Set<String> getActiveUsers(String documentId) {
        return redisTemplate.opsForSet().members(ACTIVE_USERS_KEY + documentId);
    }
}
