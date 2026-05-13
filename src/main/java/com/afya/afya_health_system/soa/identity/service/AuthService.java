package com.afya.afya_health_system.soa.identity.service;

import com.afya.afya_health_system.soa.identity.dto.MeResponse;
import com.afya.afya_health_system.soa.identity.dto.TokenResponse;
import com.afya.afya_health_system.soa.hospitalservice.model.HospitalService;
import com.afya.afya_health_system.soa.identity.model.AppUser;
import com.afya.afya_health_system.soa.identity.model.RefreshToken;
import com.afya.afya_health_system.soa.identity.model.RevokedAccessJti;
import com.afya.afya_health_system.soa.identity.config.LoginLockoutProperties;
import com.afya.afya_health_system.soa.identity.repository.AppUserRepository;
import com.afya.afya_health_system.soa.identity.repository.RefreshTokenRepository;
import com.afya.afya_health_system.soa.identity.repository.RevokedAccessJtiRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Credential checks against {@link AppUser} rows, JWT issuance, and refresh-token lifecycle persisted in DB.
 * Login and refresh reload hospital assignments from {@code user_hospital_services} so {@link TokenResponse#me()} matches {@link #me(String)} for provisioned users (not bootstrap-only).
 * <p>Français : vérifie les identifiants sur les lignes {@link AppUser}, émet les JWT et gère le cycle de vie
 * des refresh tokens persistés en base (rotation, révocation au logout) et des {@code jti} d’accès révoqués.</p>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedAccessJtiRepository revokedAccessJtiRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginLockoutProperties loginLockoutProperties;

    public AuthService(
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            AppUserRepository appUserRepository,
            RefreshTokenRepository refreshTokenRepository,
            RevokedAccessJtiRepository revokedAccessJtiRepository,
            LoginLockoutProperties loginLockoutProperties
    ) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.appUserRepository = appUserRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.revokedAccessJtiRepository = revokedAccessJtiRepository;
        this.loginLockoutProperties = loginLockoutProperties;
    }

    /**
     * Les identifiants invalides lèvent {@link ResponseStatusException} après persistance du compteur de verrouillage :
     * sans {@code noRollbackFor}, cette exception ferait rollback des mises à jour {@code failed_login_attempts} / {@code locked_until}.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public TokenResponse login(String usernameOrEmail, String password) {
        AppUser user = appUserRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Identifiants invalides"));
        Instant now = Instant.now();
        if (loginLockoutProperties.isEnabled()) {
            Instant lockedUntil = user.getLockedUntil();
            if (lockedUntil != null) {
                if (lockedUntil.isAfter(now)) {
                    throw new ResponseStatusException(UNAUTHORIZED, lockoutActiveMessage());
                }
                user.setLockedUntil(null);
                user.setFailedLoginAttempts(0);
                appUserRepository.save(user);
            }
        }
        if (!user.isActive()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Compte utilisateur désactivé. Veuillez contacter l'administrateur.");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            if (loginLockoutProperties.isEnabled()) {
                int next = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(next);
                int threshold = loginLockoutProperties.getMaxAttempts();
                if (next >= threshold) {
                    long minutes = Math.max(1L, loginLockoutProperties.getLockDurationMinutes());
                    user.setLockedUntil(now.plus(minutes, ChronoUnit.MINUTES));
                }
                appUserRepository.save(user);
            }
            throw new ResponseStatusException(UNAUTHORIZED, "Identifiants invalides");
        }
        if (loginLockoutProperties.isEnabled() && (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null)) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            appUserRepository.save(user);
        }
        refreshTokenRepository.deleteExpired(Instant.now());
        revokedAccessJtiRepository.deleteByExpiresAtBefore(Instant.now());
        AppUser userWithServices = appUserRepository.findByUsernameWithHospitalServices(user.getUsername())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Identifiants invalides"));
        String accessToken = jwtService.generateAccessToken(
                userWithServices.getUsername(),
                Map.of("roles", userWithServices.rolesAsSet(), "fullName", userWithServices.getFullName())
        );
        String refreshToken = jwtService.generateRefreshToken(userWithServices.getUsername());
        persistRefreshToken(userWithServices.getUsername(), refreshToken);
        return new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessExpirationSeconds(),
                toMeResponse(userWithServices)
        );
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        Claims claims = jwtService.parseRefreshToken(refreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Refresh token invalide"));
        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new ResponseStatusException(UNAUTHORIZED, "Refresh token expire");
        }
        if (!storedToken.getUsername().equals(claims.getSubject())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Refresh token invalide");
        }
        AppUser user = appUserRepository.findByUsernameWithHospitalServices(claims.getSubject())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Refresh token invalide"));
        if (!user.isActive()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Compte utilisateur désactivé. Veuillez contacter l'administrateur.");
        }
        String accessToken = jwtService.generateAccessToken(
                user.getUsername(),
                Map.of("roles", user.rolesAsSet(), "fullName", user.getFullName())
        );
        String newRefreshToken = jwtService.generateRefreshToken(user.getUsername());
        // Rotation strategy: old token is revoked as soon as a new one is issued.
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);
        persistRefreshToken(user.getUsername(), newRefreshToken);
        return new TokenResponse(accessToken, newRefreshToken, "Bearer", jwtService.getAccessExpirationSeconds(), toMeResponse(user));
    }

    /**
     * Optionally revokes only the submitted refresh JWT, or every active refresh row for the user, and always
     * registers the current Bearer access {@code jti} when supplied.
     * <p>Français : révocation refresh ciblée ou totale ; blocage du Bearer courant via {@code jti}.</p>
     */
    @Transactional
    public void logout(String usernameFromContext, HttpServletRequest request, String optionalRefreshTokenValue, boolean revokeAllSessions) {
        log.info("Logout: user={}, revokeAllSessions={}, targetedRefreshProvided={}",
                usernameFromContext,
                revokeAllSessions,
                optionalRefreshTokenValue != null && !optionalRefreshTokenValue.isBlank());
        revokeAccessTokenFromLogoutRequest(usernameFromContext, request);
        boolean revokeAll = revokeAllSessions || optionalRefreshTokenValue == null || optionalRefreshTokenValue.isBlank();
        if (revokeAll) {
            refreshTokenRepository.revokeAllActiveByUsername(usernameFromContext);
        } else {
            revokeSingleRefreshToken(usernameFromContext, optionalRefreshTokenValue.strip());
        }
        revokedAccessJtiRepository.deleteByExpiresAtBefore(Instant.now());
    }

    private void revokeSingleRefreshToken(String expectedUsername, String refreshTokenValue) {
        try {
            Claims c = jwtService.parseRefreshToken(refreshTokenValue);
            if (!expectedUsername.equals(c.getSubject())) {
                return;
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            return;
        }
        Optional<RefreshToken> row = refreshTokenRepository.findByToken(refreshTokenValue);
        row.filter(t -> expectedUsername.equals(t.getUsername()) && !t.isRevoked())
                .ifPresent(t -> {
                    t.setRevoked(true);
                    refreshTokenRepository.save(t);
                });
    }

    private void revokeAccessTokenFromLogoutRequest(String usernameFromContext, HttpServletRequest request) {
        readBearerToken(request)
                .ifPresent(token -> {
                    try {
                        Claims c = jwtService.parseAccessToken(token);
                        if (!usernameFromContext.equals(c.getSubject())) {
                            return;
                        }
                        String jti = c.getId();
                        if (jti == null || jti.isBlank()) {
                            return;
                        }
                        Instant exp = c.getExpiration().toInstant();
                        if (!exp.isAfter(Instant.now())) {
                            return;
                        }
                        if (!revokedAccessJtiRepository.existsById(jti)) {
                            revokedAccessJtiRepository.save(new RevokedAccessJti(jti, exp, usernameFromContext));
                        }
                    } catch (JwtException | IllegalArgumentException ignored) {
                        // Bearer absent ou illisible : on garde tout de même la révocation refresh + context utilisateur.
                    }
                });
    }

    private static Optional<String> readBearerToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

    public MeResponse me(String username) {
        AppUser user = appUserRepository.findByUsernameWithHospitalServices(username)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Utilisateur introuvable"));
        if (!user.isActive()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Compte utilisateur désactivé. Veuillez contacter l'administrateur.");
        }
        return toMeResponse(user);
    }

    private static MeResponse toMeResponse(AppUser user) {
        List<Long> hospitalServiceIds = user.getHospitalServices() == null ? List.of()
                : user.getHospitalServices().stream().map(HospitalService::getId).sorted().toList();
        List<String> hospitalServiceNames = user.getHospitalServices() == null ? List.of()
                : user.getHospitalServices().stream()
                .map(HospitalService::getName)
                .sorted(Comparator.comparing(String::toLowerCase))
                .toList();
        return new MeResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.rolesAsSet(),
                hospitalServiceIds,
                hospitalServiceNames
        );
    }

    private void persistRefreshToken(String username, String tokenValue) {
        RefreshToken token = new RefreshToken();
        token.setToken(tokenValue);
        token.setUsername(username);
        token.setExpiresAt(Instant.now().plusSeconds(jwtService.getRefreshExpirationSeconds()));
        token.setRevoked(false);
        refreshTokenRepository.save(token);
    }

    private static String lockoutActiveMessage() {
        return "Compte temporairement verrouillé après plusieurs échecs de connexion. Veuillez contacter l'administrateur.";
    }
}
