package com.ecommerce.authservice.service;

import com.ecommerce.authservice.config.JwtConfig;
import com.ecommerce.authservice.entity.RefreshToken;
import com.ecommerce.authservice.entity.User;
import com.ecommerce.authservice.exception.AuthException;
import com.ecommerce.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh_token:";
    private final StringRedisTemplate redisTemplate;
    private final JwtConfig jwtConfig;
    private final UserRepository userRepository;

    public RefreshToken createRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        long expirationDays = jwtConfig.getRefreshTokenExpirationDays();
        
        redisTemplate.opsForValue().set(
                key(token),
                user.getId().toString(),
                Duration.ofDays(expirationDays)
        );

        log.debug("Created refresh token in Redis for userId={}", user.getId());
        return RefreshToken.builder()
                .user(user)
                .token(token)
                .expiryDate(Instant.now().plus(expirationDays, ChronoUnit.DAYS))
                .build();
    }

    public RefreshToken rotateRefreshToken(String token) {
        String redisKey = key(token);

        String userIdStr = redisTemplate.opsForValue().get(redisKey);

        if (userIdStr == null) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }

        UUID userId = UUID.fromString(userIdStr);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "User not found"));

        redisTemplate.delete(redisKey);

        return createRefreshToken(user);
    }

    public void revokeToken(String token) {
        redisTemplate.delete(key(token));
        log.debug("Revoked refresh token={}", token);
    }

    private String key(String token) {
        return REFRESH_TOKEN_PREFIX + token;
    }
}
