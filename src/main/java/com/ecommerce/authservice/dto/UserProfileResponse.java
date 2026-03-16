package com.ecommerce.authservice.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight projection of the response returned by User Service's
 * {@code POST /users} endpoint.
 *
 * <p>Only the fields needed by Auth Service are captured; unmapped fields
 * in the full {@code UserResponse} are silently ignored by Jackson.
 */
public record UserProfileResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        Instant createdAt
) {}
