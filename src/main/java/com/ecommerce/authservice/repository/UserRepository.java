package com.ecommerce.authservice.repository;

import com.ecommerce.authservice.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"roles", "roles.role"})
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"roles", "roles.role"})
    Optional<User> findByPhone(String phone);

    @EntityGraph(attributePaths = {"roles", "roles.role"})
    Optional<User> findWithRolesById(UUID id);
}
