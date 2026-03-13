package com.ecommerce.authservice.repository;

import com.ecommerce.authservice.entity.RefreshToken;
import com.ecommerce.authservice.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @EntityGraph(attributePaths = {"user", "user.roles", "user.roles.role"})
    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);
}
