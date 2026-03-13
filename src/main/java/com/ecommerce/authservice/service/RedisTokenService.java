package com.ecommerce.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh_token:";
    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    public void saveRefreshToken(String userId, String refreshToken) {
        String key = refreshKey(userId);
        String existing = redisTemplate.opsForValue().get(key);
        if (existing != null) {
            redisTemplate.delete(refreshTokenKey(existing));
        }

        redisTemplate.opsForValue().set(key, refreshToken, REFRESH_TTL);
        redisTemplate.opsForValue().set(refreshTokenKey(refreshToken), userId, REFRESH_TTL);
        log.debug("Stored refresh token for userId={}", userId);
    }

    public String getRefreshToken(String userId) {
        return redisTemplate.opsForValue().get(refreshKey(userId));
    }

    public void deleteRefreshToken(String userId) {
        String key = refreshKey(userId);
        String existing = redisTemplate.opsForValue().get(key);
        if (existing != null) {
            redisTemplate.delete(refreshTokenKey(existing));
        }
        redisTemplate.delete(key);
        log.debug("Deleted refresh token for userId={}", userId);
    }

    public String getUserIdByRefreshToken(String refreshToken) {
        return redisTemplate.opsForValue().get(refreshTokenKey(refreshToken));
    }

    private String refreshKey(String userId) {
        return REFRESH_PREFIX + userId;
    }

    private String refreshTokenKey(String token) {
        return REFRESH_TOKEN_PREFIX + token;
    }
}
