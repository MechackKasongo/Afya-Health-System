package com.afya.afya_health_system.soa.identity.repository;

import com.afya.afya_health_system.soa.identity.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCode(String code);
}
