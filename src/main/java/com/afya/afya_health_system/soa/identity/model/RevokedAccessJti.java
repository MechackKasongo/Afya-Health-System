package com.afya.afya_health_system.soa.identity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Access-token {@code jti} revoked at logout until original expiration (JWT still signed but rejected by filter).
 * <p>Français : liste des identifiants de jetons d’accès révoqués ; entrée purgeable après {@code expiresAt}.</p>
 */
@Entity
@Table(name = "revoked_access_jti", indexes = {
        @Index(name = "idx_revoked_access_jti_expires", columnList = "expiresAt")
})
public class RevokedAccessJti {

    @Id
    @Column(length = 128)
    private String jti;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, length = 80)
    private String username;

    public RevokedAccessJti() {
    }

    public RevokedAccessJti(String jti, Instant expiresAt, String username) {
        this.jti = jti;
        this.expiresAt = expiresAt;
        this.username = username;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
