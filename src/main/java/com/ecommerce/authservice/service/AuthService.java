package com.ecommerce.authservice.service;

import com.ecommerce.authservice.client.UserServiceClient;
import com.ecommerce.authservice.config.KafkaTopicsProperties;
import com.ecommerce.authservice.dto.*;
import com.ecommerce.authservice.entity.*;
import com.ecommerce.authservice.event.EventPublisher;
import com.ecommerce.authservice.exception.AuthException;
import com.ecommerce.authservice.repository.RoleRepository;
import com.ecommerce.authservice.repository.UserRepository;
import com.ecommerce.authservice.repository.UserRoleRepository;
import com.ecommerce.events.LoginEvent;
import com.ecommerce.events.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;
    private final JwtBlacklistService jwtBlacklistService;
    private final EventPublisher eventPublisher;          // no-op in REST mode
    private final KafkaTopicsProperties kafkaTopicsProperties;
    private final UserServiceClient userServiceClient;   // REST-mode: sync profile creation

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase();
        userRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
            throw new AuthException(HttpStatus.CONFLICT, "Email already registered");
        });

        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new AuthException(HttpStatus.CONFLICT, "Phone already registered");
        }

        RoleName requestedRole;
        try {
            requestedRole = RoleName.valueOf(request.getRole().toUpperCase());
            if (requestedRole == RoleName.ADMIN) {
                throw new AuthException(HttpStatus.BAD_REQUEST, "Invalid role");
            }
        } catch (IllegalArgumentException ex) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Invalid role");
        }

        User user = User.builder()
                .email(normalizedEmail)
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        Role role = roleRepository.findByName(requestedRole)
                .orElseGet(() -> roleRepository.save(Role.builder().name(requestedRole).build()));

        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(savedUser.getId(), role.getId()))
                .user(savedUser)
                .role(role)
                .build();
        userRoleRepository.save(userRole);

        // ── REST-mode: synchronously propagate profile to User Service ──────────
        // This replaces the Kafka UserCreatedEvent. The call is fire-and-continue:
        // a failure here does NOT roll back the auth registration.
        UserProfileRequest profileRequest = new UserProfileRequest(
                savedUser.getId(),
                savedUser.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                savedUser.getPhone()
        );
        userServiceClient.createUserProfile(profileRequest);

        // ── Kafka no-op (preserved for easy re-enable) ───────────────────────────
        UserCreatedEvent event = new UserCreatedEvent(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getPhone(),
                request.getFirstName(),
                request.getLastName(),
                requestedRole.name(),
                savedUser.getCreatedAt()
        );
        eventPublisher.publishAfterCommit(kafkaTopicsProperties.getUserCreated(), event);

        log.info("Registered user with email={}", savedUser.getEmail());
        return RegisterResponse.builder()
                .message("User registered successfully")
                .userId(savedUser.getId())
                .role(requestedRole.name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase();
        if (loginAttemptService.isBlocked(normalizedEmail)) {
            throw new AuthException(HttpStatus.TOO_MANY_REQUESTS, "Account locked due to failed attempts");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword()));
        } catch (Exception ex) {
            loginAttemptService.loginFailed(normalizedEmail);
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .or(() -> userRepository.findByPhone(request.getEmail()))
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        List<String> roles = user.getRoles().stream()
                .map(userRole -> userRole.getRole().getName().name())
                .toList();

        JwtToken accessToken = jwtService.generateAccessToken(user.getId().toString(), user.getEmail(), roles);
        RefreshToken refreshTokenEntity = refreshTokenService.createRefreshToken(user);
        String refreshToken = refreshTokenEntity.getToken();
        loginAttemptService.loginSucceeded(normalizedEmail);

        LoginEvent loginEvent = new LoginEvent(user.getId(), user.getEmail(), Instant.now());
        eventPublisher.publish(kafkaTopicsProperties.getUserLogin(), loginEvent);

        log.info("User logged in email={}", user.getEmail());
        return AuthResponse.builder()
                .accessToken(accessToken.token())
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresAt(accessToken.expiresAt())
                .build();
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshTokenEntity = refreshTokenService.rotateRefreshToken(request.getRefreshToken());
        User user = refreshTokenEntity.getUser();

        List<String> roles = user.getRoles().stream()
                .map(userRole -> userRole.getRole().getName().name())
                .toList();

        JwtToken accessToken = jwtService.generateAccessToken(user.getId().toString(), user.getEmail(), roles);
        String newRefreshToken = refreshTokenEntity.getToken();

        return AuthResponse.builder()
                .accessToken(accessToken.token())
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresAt(accessToken.expiresAt())
                .build();
    }

    public void logout(String refreshToken, String accessToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revokeToken(refreshToken);
        }
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                String jti = jwtService.getJti(accessToken);
                Instant expiresAt = jwtService.getExpiration(accessToken);
                jwtBlacklistService.blacklistToken(jti, expiresAt);
            } catch (Exception ex) {
                log.debug("Skipping access token blacklist: {}", ex.getMessage());
            }
        }
        log.info("Logout completed");
    }

    public TokenValidationResponse validate(String token) {
        if (!jwtService.isTokenValid(token)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        String jti = jwtService.getJti(token);
        if (jwtBlacklistService.isBlacklisted(jti)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Token revoked");
        }

        var claims = jwtService.parseClaims(token);
        List<String> roles = claims.get("roles", List.class);

        return TokenValidationResponse.builder()
                .valid(true)
                .userId(claims.get("uid", String.class))
                .email(claims.get("email", String.class))
                .role(roles != null && !roles.isEmpty() ? roles.get(0) : null)
                .roles(roles)
                .expiresAt(claims.getExpiration().toInstant())
                .build();
    }
}
