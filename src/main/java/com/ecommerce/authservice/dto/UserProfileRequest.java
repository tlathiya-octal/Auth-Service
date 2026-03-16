package com.ecommerce.authservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * Request body sent from Auth Service → User Service when creating a user profile
 * synchronously after registration.
 *
 * <p>Field names intentionally match the {@code UserRequest} record in User Service
 * so the JSON payload is deserialized without any mapping configuration on the
 * receiving side.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProfileRequest(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String phoneNumber
) {}
