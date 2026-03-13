package com.ecommerce.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final String OTP_PREFIX = "auth:otp:";
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    public String generateOtp() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }

    public void storeOtp(String email, String otp) {
        String key = otpKey(email);
        redisTemplate.opsForValue().set(key, otp, OTP_TTL);
        log.debug("Stored OTP for email={}", email);
    }

    public boolean validateOtp(String email, String otp) {
        String key = otpKey(email);
        String stored = redisTemplate.opsForValue().get(key);
        boolean valid = stored != null && stored.equals(otp);
        if (valid) {
            redisTemplate.delete(key);
        }
        return valid;
    }

    private String otpKey(String email) {
        return OTP_PREFIX + email.toLowerCase();
    }
}
