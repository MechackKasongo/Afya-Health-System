package com.afya.afya_health_system.soa.identity.controller;

import com.afya.afya_health_system.soa.identity.dto.LoginRequest;
import com.afya.afya_health_system.soa.identity.dto.LogoutRequest;
import com.afya.afya_health_system.soa.identity.dto.MeResponse;
import com.afya.afya_health_system.soa.identity.dto.RefreshRequest;
import com.afya.afya_health_system.soa.identity.dto.TokenResponse;
import com.afya.afya_health_system.soa.identity.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication API: credentials exchange, token refresh, session teardown, and current user profile.
 * Protected operations require an {@code Authorization} header with a Bearer access token.
 * <p>Français : API d'authentification (login, rafraîchissement, déconnexion, profil). Les appels
 * protégés exigent un en-tête {@code Authorization} avec un jeton d'accès Bearer.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Issues a new access + refresh token pair after password validation.
     * <p>Français : délivre une paire access + refresh après vérification du mot de passe.</p>
     */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    /**
     * Rotates refresh token and returns a fresh access token (old refresh is revoked).
     * <p>Français : rotation du refresh ; l'ancien jeton est révoqué.</p>
     */
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    /**
     * Revokes persisted refresh tokens and the {@code jti} of the current Bearer access JWT when provided.
     * <p>Français : révoque les refresh et, si l'en-tête {@code Authorization} est présent, le jeton d'accès actuel.</p>
     */
    @PostMapping("/logout")
    public void logout(
            Authentication authentication,
            HttpServletRequest request,
            @RequestBody(required = false) LogoutRequest body
    ) {
        boolean revokeAllSessions = body == null
                || body.revokeAllSessions() == null
                || Boolean.TRUE.equals(body.revokeAllSessions());
        String refreshToken = body != null ? body.refreshToken() : null;
        authService.logout(authentication.getName(), request, refreshToken, revokeAllSessions);
    }

    /**
     * Returns profile for the user resolved from the JWT.
     * <p>Français : profil de l'utilisateur issu du JWT.</p>
     */
    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}
