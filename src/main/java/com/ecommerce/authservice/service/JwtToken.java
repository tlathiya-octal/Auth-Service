package com.ecommerce.authservice.service;

import java.time.Instant;

public record JwtToken(String token, Instant expiresAt, String jti) {
}
