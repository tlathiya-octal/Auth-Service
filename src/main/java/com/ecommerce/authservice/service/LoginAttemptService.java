package com.ecommerce.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final String LOGIN_ATTEMPT_PREFIX = "auth:login_attempt:";
    private static final Duration ATTEMPT_TTL = Duration.ofMinutes(15);
    private static final int MAX_ATTEMPTS = 5;

    private final StringRedisTemplate redisTemplate;

    public void loginSucceeded(String email) {
        redisTemplate.delete(key(email));
    }

    public void loginFailed(String email) {
        String key = key(email);
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(key, ATTEMPT_TTL);
        }
        log.debug("Login failed for email={}, attempts={}", email, attempts);
    }

    public boolean isBlocked(String email) {
        String value = redisTemplate.opsForValue().get(key(email));
        if (value == null) {
            return false;
        }
        try {
            return Integer.parseInt(value) >= MAX_ATTEMPTS;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String key(String email) {
        return LOGIN_ATTEMPT_PREFIX + email.toLowerCase();
    }
}
