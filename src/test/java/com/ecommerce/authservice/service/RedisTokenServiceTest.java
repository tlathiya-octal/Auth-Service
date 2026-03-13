package com.ecommerce.authservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisTokenService redisTokenService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        redisTokenService = new RedisTokenService(redisTemplate);
    }

    @Test
    void saveRefreshTokenReplacesExisting() {
        when(valueOperations.get("auth:refresh:user-1")).thenReturn("old-token");

        redisTokenService.saveRefreshToken("user-1", "new-token");

        verify(redisTemplate).delete("auth:refresh_token:old-token");
        verify(valueOperations).set(eq("auth:refresh:user-1"), eq("new-token"), any(Duration.class));
        verify(valueOperations).set(eq("auth:refresh_token:new-token"), eq("user-1"), any(Duration.class));
    }

    @Test
    void deleteRefreshTokenRemovesReverseKey() {
        when(valueOperations.get("auth:refresh:user-1")).thenReturn("token-x");

        redisTokenService.deleteRefreshToken("user-1");

        verify(redisTemplate).delete("auth:refresh_token:token-x");
        verify(redisTemplate).delete("auth:refresh:user-1");
    }
}
