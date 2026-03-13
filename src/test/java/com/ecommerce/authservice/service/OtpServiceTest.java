package com.ecommerce.authservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(redisTemplate);
    }

    @Test
    void generateOtpReturnsSixDigits() {
        String otp = otpService.generateOtp();
        assertThat(otp).hasSize(6);
    }

    @Test
    void validateOtpDeletesOnSuccess() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:otp:test@example.com")).thenReturn("123456");

        boolean result = otpService.validateOtp("test@example.com", "123456");

        assertThat(result).isTrue();
        verify(redisTemplate).delete("auth:otp:test@example.com");
    }
}
