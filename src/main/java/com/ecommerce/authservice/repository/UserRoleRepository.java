package com.ecommerce.authservice.repository;

import com.ecommerce.authservice.entity.UserRole;
import com.ecommerce.authservice.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
}
