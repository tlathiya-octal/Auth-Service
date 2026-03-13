package com.ecommerce.authservice.dto;

import lombok.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {

    private String message;
    private UUID userId;
    private String role;
}
