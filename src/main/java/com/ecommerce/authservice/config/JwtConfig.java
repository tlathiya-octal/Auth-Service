package com.ecommerce.authservice.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    @NotBlank
    private String issuer;

    @NotBlank
    private String secret;

    @Min(1)
    private long accessTokenExpirationMinutes;

    @Min(1)
    private long refreshTokenExpirationDays;
}
