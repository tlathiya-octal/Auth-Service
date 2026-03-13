package com.ecommerce.authservice.controller;

import com.ecommerce.authservice.dto.*;
import com.ecommerce.authservice.exception.AuthException;
import com.ecommerce.authservice.service.AuthService;
import com.ecommerce.authservice.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        String accessToken = resolveToken(authHeader);
        authService.logout(request.getRefreshToken(), accessToken);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/validate")
    public TokenValidationResponse validate(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        String token = resolveToken(authHeader);
        if (token == null) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Missing Authorization header");
        }
        return authService.validate(token);
    }

    @PostMapping("/otp/generate")
    public ResponseEntity<String> generateOtp(@Valid @RequestBody OtpRequest request) {
        String otp = otpService.generateOtp();
        otpService.storeOtp(request.getEmail(), otp);
        return ResponseEntity.accepted().body("OTP generated");
    }

    @PostMapping("/otp/validate")
    public ResponseEntity<String> validateOtp(@Valid @RequestBody OtpValidationRequest request) {
        boolean valid = otpService.validateOtp(request.getEmail(), request.getOtp());
        if (!valid) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid OTP");
        }
        return ResponseEntity.ok("OTP validated");
    }

    private String resolveToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
}
