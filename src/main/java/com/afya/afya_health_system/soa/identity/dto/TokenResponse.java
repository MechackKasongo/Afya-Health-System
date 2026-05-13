package com.afya.afya_health_system.soa.identity.dto;

/**
 * Token pair returned by {@code /auth/login} and {@code /auth/refresh}. Field {@code me} mirrors {@code GET /auth/me}
 * (persisted user row including hospital assignments) so clients need not chain an extra call after login.
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        MeResponse me
) {
}
