package com.afya.afya_health_system.soa.identity.model;

import com.afya.afya_health_system.soa.hospitalservice.model.HospitalService;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Persisted application user for login. Password is stored as a BCrypt hash.
 * Roles are linked from a dedicated role table.
 * <p>Français : utilisateur applicatif persisté. Mot de passe haché (BCrypt). Les rôles sont liés via une
 * table dédiée.</p>
 */
@Entity
@Table(name = "app_users")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(unique = true, length = 160)
    private String email;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    /**
     * Hospital units assigned to clinical staff for admission scope (many-to-many join table).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    @JoinTable(
            name = "user_hospital_services",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "hospital_service_id")
    )
    private Set<HospitalService> hospitalServices;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private int failedLoginAttempts;

    @Column
    private Instant lockedUntil;

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        // Hibernate mutates role collections during merge/save — never store immutable sets (Set.of et al.).
        if (roles == null) {
            this.roles = null;
        } else {
            this.roles = new HashSet<>(roles);
        }
    }

    public Set<HospitalService> getHospitalServices() {
        return hospitalServices;
    }

    public void setHospitalServices(Set<HospitalService> hospitalServices) {
        if (hospitalServices == null) {
            this.hospitalServices = null;
        } else {
            this.hospitalServices = new HashSet<>(hospitalServices);
        }
    }

    public Set<String> rolesAsSet() {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        return roles.stream()
                .map(Role::getCode)
                .filter(role -> role != null && !role.isBlank())
                .collect(Collectors.toSet());
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }
}
