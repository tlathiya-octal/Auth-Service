package com.ecommerce.authservice.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationResponse {

    private boolean valid;
    private String userId;
    private String email;
    private String role;
    private List<String> roles;
    private Instant expiresAt;
}
