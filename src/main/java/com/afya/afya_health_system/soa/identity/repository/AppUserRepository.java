package com.afya.afya_health_system.soa.identity.repository;

import com.afya.afya_health_system.soa.identity.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    @Query("SELECT DISTINCT u FROM AppUser u LEFT JOIN FETCH u.hospitalServices WHERE u.username = :username")
    Optional<AppUser> findByUsernameWithHospitalServices(@Param("username") String username);
    Optional<AppUser> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Page<AppUser> findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username,
            String fullName,
            String email,
            Pageable pageable
    );

    @Query("SELECT COALESCE(MAX(u.id), 0) FROM AppUser u")
    long findMaxId();
}
