package com.ecommerce.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtBlacklistService {

    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void blacklistToken(String jwtId, Instant expiresAt) {
        if (jwtId == null || jwtId.isBlank()) {
            return;
        }
        long ttlSeconds = Duration.between(Instant.now(), expiresAt).getSeconds();
        if (ttlSeconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(key(jwtId), "1", ttlSeconds, TimeUnit.SECONDS);
        log.debug("Blacklisted jwtId={} for {} seconds", jwtId, ttlSeconds);
    }

    public boolean isBlacklisted(String jwtId) {
        if (jwtId == null || jwtId.isBlank()) {
            return false;
        }
        Boolean exists = redisTemplate.hasKey(key(jwtId));
        return Boolean.TRUE.equals(exists);
    }

    private String key(String jwtId) {
        return BLACKLIST_PREFIX + jwtId;
    }
}
