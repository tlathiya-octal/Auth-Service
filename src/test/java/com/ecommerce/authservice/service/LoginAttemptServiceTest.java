package com.ecommerce.authservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        loginAttemptService = new LoginAttemptService(redisTemplate);
    }

    @Test
    void loginFailedSetsExpiryOnFirstAttempt() {
        when(valueOperations.increment("auth:login_attempt:test@example.com")).thenReturn(1L);

        loginAttemptService.loginFailed("test@example.com");

        verify(redisTemplate).expire(eq("auth:login_attempt:test@example.com"), any());
    }

    @Test
    void isBlockedReturnsTrueWhenMaxReached() {
        when(valueOperations.get("auth:login_attempt:test@example.com")).thenReturn("5");

        boolean blocked = loginAttemptService.isBlocked("test@example.com");

        assertThat(blocked).isTrue();
    }
}
