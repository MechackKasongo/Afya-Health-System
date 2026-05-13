package com.afya.afya_health_system.soa.identity.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Creates and parses JWT access/refresh tokens (HS512).
 * Claim {@code type} distinguishes access vs refresh; user display data lives in access claims only.
 * <p>Français : construction et lecture des JWT (HS512). Le claim {@code type} sépare access et refresh ;
 * le nom complet et les rôles ne sont portés que par le jeton d'accès.</p>
 */
@Service
public class JwtService {

    private final SecretKey accessSecretKey;
    private final SecretKey refreshSecretKey;
    private final long accessExpirationSeconds;
    private final long refreshExpirationSeconds;

    private static final int MIN_UTF8_BYTES_HS512 = 64;

    public JwtService(
            @Value("${app.jwt.access-secret}") String accessSecret,
            @Value("${app.jwt.refresh-secret}") String refreshSecret,
            @Value("${app.jwt.access-expiration-seconds:3600}") long accessExpirationSeconds,
            @Value("${app.jwt.refresh-expiration-seconds:2592000}") long refreshExpirationSeconds
    ) {
        String access = requireJwtUtf8Secret(accessSecret, "JWT_ACCESS_SECRET", "app.jwt.access-secret");
        String refresh = requireJwtUtf8Secret(refreshSecret, "JWT_REFRESH_SECRET", "app.jwt.refresh-secret");
        this.accessSecretKey = Keys.hmacShaKeyFor(access.getBytes(StandardCharsets.UTF_8));
        this.refreshSecretKey = Keys.hmacShaKeyFor(refresh.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationSeconds = accessExpirationSeconds;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    /**
     * Fails fast with a readable message instead of JJWT internals; returns {@code strip()}'ed secret for signing.
     * <p>Français : contrôle présence et longueur UTF-8 (≥64 octets) exigées par HS512.</p>
     */
    private static String requireJwtUtf8Secret(String raw, String envVarHint, String propertyHint) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException(
                    "Configuration JWT invalide : "
                            + envVarHint
                            + " ("
                            + propertyHint
                            + ") doit être défini et non vide."
            );
        }
        String secret = raw.strip();
        if (secret.isEmpty()) {
            throw new IllegalStateException(
                    "Configuration JWT invalide : " + envVarHint + " ne doit pas être composé uniquement d'espaces."
            );
        }
        int utf8Length = secret.getBytes(StandardCharsets.UTF_8).length;
        if (utf8Length < MIN_UTF8_BYTES_HS512) {
            throw new IllegalStateException(
                    "Configuration JWT invalide : "
                            + envVarHint
                            + " doit comporter au moins "
                            + MIN_UTF8_BYTES_HS512
                            + " octets en UTF-8 pour HS512 (actuellement "
                            + utf8Length
                            + ")."
            );
        }
        return secret;
    }

    /**
     * Short-lived token sent on every API call (roles, fullName, etc.).
     * <p>Français : jeton court, envoyé à chaque appel API (rôles, nom affiché, etc.).</p>
     */
    /**
     * Every access JWT gets a stable {@code jti} via {@link io.jsonwebtoken.JwtBuilder#setId(String)} for server-side revocation (logout).
     * <p>Français : chaque jeton d’accès possède un {@code jti} pour pouvoir être invalidé côté serveur.</p>
     */
    public String generateAccessToken(String subject, Map<String, Object> claims) {
        Map<String, Object> accessClaims = new HashMap<>(claims);
        accessClaims.put("type", "access");
        String jti = UUID.randomUUID().toString();
        return generateSignedToken(subject, accessClaims, accessSecretKey, accessExpirationSeconds, jti);
    }

    /**
     * Long-lived token used only to obtain new pairs; must also match a persisted row for rotation.
     * Claim {@code jti} guarantees uniqueness across logins within the same second (DB unique on token column).
     * <p>Français : jeton longue durée pour nouvelles paires ; doit exister en base pour la rotation. Le claim
     * {@code jti} évite les collisions si plusieurs connexions partagent la même seconde {@code issuedAt}.</p>
     */
    public String generateRefreshToken(String subject) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("jti", UUID.randomUUID().toString());
        return generateSignedToken(subject, claims, refreshSecretKey, refreshExpirationSeconds, null);
    }

    public Claims parseRefreshToken(String token) {
        Claims claims = parseToken(token, refreshSecretKey);
        if (!"refresh".equals(claims.get("type"))) {
            throw new IllegalArgumentException("Type de token invalide");
        }
        return claims;
    }

    public Claims parseAccessToken(String token) {
        Claims claims = parseToken(token, accessSecretKey);
        Object type = claims.get("type");
        if (type != null && !"access".equals(type.toString())) {
            throw new IllegalArgumentException("Type de token invalide");
        }
        return claims;
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationSeconds;
    }

    public long getRefreshExpirationSeconds() {
        return refreshExpirationSeconds;
    }

    /**
     * @param jti when non-null, sets the standard JWT {@code jti} claim (access tokens); refresh embeds jti via {@code claims} map.
     */
    private String generateSignedToken(
            String subject,
            Map<String, Object> claims,
            SecretKey key,
            long expirationSeconds,
            String jti
    ) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .setSubject(subject)
                .addClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(expirationSeconds)));
        if (jti != null && !jti.isBlank()) {
            builder.setId(jti);
        }
        return builder.signWith(key, SignatureAlgorithm.HS512).compact();
    }

    private Claims parseToken(String token, SecretKey key) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
