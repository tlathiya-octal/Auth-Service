package com.ecommerce.authservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private JwtBlacklistService jwtBlacklistService;

    @BeforeEach
    void setUp() {
        jwtBlacklistService = new JwtBlacklistService(redisTemplate);
    }

    @Test
    void blacklistTokenSkipsExpired() {
        jwtBlacklistService.blacklistToken("jti-1", Instant.now().minusSeconds(10));

        verify(valueOperations, never()).set(any(), any(), anyLong(), any());
    }
}
