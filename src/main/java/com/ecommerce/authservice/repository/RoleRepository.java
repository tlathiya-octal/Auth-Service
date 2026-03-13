package com.ecommerce.authservice.repository;

import com.ecommerce.authservice.entity.Role;
import com.ecommerce.authservice.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(RoleName name);
}
