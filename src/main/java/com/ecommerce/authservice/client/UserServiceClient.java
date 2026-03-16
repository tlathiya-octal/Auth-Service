package com.ecommerce.authservice.client;

import com.ecommerce.authservice.dto.UserProfileRequest;
import com.ecommerce.authservice.dto.UserProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Synchronous REST client for User Service communication.
 *
 * <p>Used by {@link com.ecommerce.authservice.service.AuthService} to propagate
 * a newly registered user's profile to the User Service immediately after
 * the auth record is persisted — replacing the previous Kafka event.
 *
 * <p>This client is intentionally lightweight: it uses a plain {@link RestTemplate}
 * (configured in {@link com.ecommerce.authservice.config.RestClientConfig}) with
 * sensible connect/read timeouts so it never blocks the caller thread indefinitely.
 */
@Slf4j
@Component
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private final String userServiceBaseUrl;

    public UserServiceClient(
            RestTemplate restTemplate,
            @Value("${app.services.user-service.base-url}") String userServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.userServiceBaseUrl = userServiceBaseUrl;
    }

    /**
     * Creates a user profile in the User Service.
     *
     * <p>Failures are handled defensively:
     * <ul>
     *   <li>409 Conflict → profile already exists (idempotent — logged and ignored).</li>
     *   <li>Other 4xx/5xx → logged as a warning; registration itself is NOT rolled back
     *       because the auth record is already committed. The profile can be retried later.</li>
     *   <li>Network errors → same defensive handling.</li>
     * </ul>
     *
     * @param request the profile data derived from the registration request.
     * @return the created {@link UserProfileResponse}, or {@code null} on non-fatal failure.
     */
    public UserProfileResponse createUserProfile(UserProfileRequest request) {
        String url = userServiceBaseUrl + "/users";
        try {
            ResponseEntity<UserProfileResponse> response =
                    restTemplate.postForEntity(url, request, UserProfileResponse.class);

            log.info("User profile created in user-service for email={} status={}",
                    request.email(), response.getStatusCode());
            return response.getBody();

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                // Profile already exists — idempotent, safe to ignore
                log.info("User profile already exists in user-service for email={} — skipping",
                        request.email());
                return null;
            }
            log.warn("Failed to create user profile in user-service for email={} status={} body={}",
                    request.email(), ex.getStatusCode(), ex.getResponseBodyAsString());
            return null;

        } catch (ResourceAccessException ex) {
            log.warn("user-service is unreachable — profile creation skipped for email={}. Cause: {}",
                    request.email(), ex.getMessage());
            return null;
        }
    }
}
