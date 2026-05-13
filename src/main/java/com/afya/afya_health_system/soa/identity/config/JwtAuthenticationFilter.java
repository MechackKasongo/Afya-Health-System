package com.afya.afya_health_system.soa.identity.config;

import com.afya.afya_health_system.soa.identity.repository.RevokedAccessJtiRepository;
import com.afya.afya_health_system.soa.identity.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Parses {@code Authorization: Bearer} access tokens and populates {@link SecurityContextHolder} when valid.
 * Tokens whose {@code jti} is present in {@link RevokedAccessJtiRepository} (logout blocklist) are treated as absent.
 * Missing or malformed headers pass through unauthenticated; bad tokens clear the context to avoid partial auth.
 * <p>Français : lit le jeton Bearer, valide l'accès et remplit le contexte Spring Security. Un {@code jti} révoqué (logout)
 * est ignoré comme un jeton absent. En-tête absent ou invalide : pas d'authentification ; jeton rejeté : contexte effacé.</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final RevokedAccessJtiRepository revokedAccessJtiRepository;

    public JwtAuthenticationFilter(JwtService jwtService, RevokedAccessJtiRepository revokedAccessJtiRepository) {
        this.jwtService = jwtService;
        this.revokedAccessJtiRepository = revokedAccessJtiRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Valid access token → Spring Authentication / Jeton d'accès valide → objet Authentication Spring.
            Claims claims = jwtService.parseAccessToken(token);

            String jti = claims.getId();
            if (jti != null && !jti.isBlank() && revokedAccessJtiRepository.existsById(jti)) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            String username = claims.getSubject();

            if (username != null && !username.isBlank() && SecurityContextHolder.getContext().getAuthentication() == null) {
                Collection<SimpleGrantedAuthority> authorities = extractAuthorities(claims);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        authorities
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            // Never authenticate on bad JWT / Ne jamais authentifier avec un JWT invalide.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Maps {@code roles} claim (collection of strings) into {@link SimpleGrantedAuthority} list.
     * <p>Français : transforme le claim {@code roles} en autorités Spring.</p>
     */
    private Collection<SimpleGrantedAuthority> extractAuthorities(Claims claims) {
        Object rolesObj = claims.get("roles");
        if (!(rolesObj instanceof Collection<?> rolesCollection)) {
            return Collections.emptyList();
        }
        return rolesCollection.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(role -> !role.isBlank())
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
