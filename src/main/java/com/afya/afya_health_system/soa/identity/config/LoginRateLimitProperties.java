package com.afya.afya_health_system.soa.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * In-memory throttle for {@code POST /api/v1/auth/login} keyed by resolved client IP.
 * <p>Français : limite débit mémoire pour le login ; clé = IP client résolue.</p>
 */
@ConfigurationProperties(prefix = "app.security.login-rate-limit")
public class LoginRateLimitProperties {

    /** When false, all login attempts pass (integration tests typically disable this). */
    private boolean enabled = true;

    /**
     * Tokens per {@link #refillDurationMinutes} per IP ({@link io.github.bucket4j.Bucket} refill).
     * <p>Français : tentatives maximales par période et par adresse IP.</p>
     */
    private int requestsPerMinute = 15;

    /** Length of each refill window used with {@link #requestsPerMinute}. */
    private int refillDurationMinutes = 1;

    /**
     * If true, the first comma-separated hop in {@code X-Forwarded-For} is trusted as client IP (use only behind your own reverse-proxy).
     * <p>Français : à activer uniquement derrière un reverse-proxy de confiance.</p>
     */
    private boolean trustForwardedForHeader = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }

    public void setRequestsPerMinute(int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    public int getRefillDurationMinutes() {
        return refillDurationMinutes;
    }

    public void setRefillDurationMinutes(int refillDurationMinutes) {
        this.refillDurationMinutes = refillDurationMinutes;
    }

    public boolean isTrustForwardedForHeader() {
        return trustForwardedForHeader;
    }

    public void setTrustForwardedForHeader(boolean trustForwardedForHeader) {
        this.trustForwardedForHeader = trustForwardedForHeader;
    }
}
