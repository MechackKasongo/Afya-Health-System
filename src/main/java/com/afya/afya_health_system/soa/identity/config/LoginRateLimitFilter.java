package com.afya.afya_health_system.soa.identity.config;

import com.afya.afya_health_system.soa.identity.service.LoginIpRateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Blocks excessive {@code POST /api/v1/auth/login} attempts per client IP (HTTP 429),
 * aligned with JSON error envelope used elsewhere ({@link com.afya.afya_health_system.soa.common.config.GlobalExceptionHandler}).
 * <p>Français : limite les connexions par IP ; réponse 429 au même format JSON que les autres erreurs API.</p>
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";

    private final LoginIpRateLimiter loginIpRateLimiter;
    private final LoginRateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public LoginRateLimitFilter(
            LoginIpRateLimiter loginIpRateLimiter,
            LoginRateLimitProperties properties,
            ObjectMapper objectMapper
    ) {
        this.loginIpRateLimiter = loginIpRateLimiter;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !properties.isEnabled() || !isLoginPost(request);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String clientKey = resolveClientKey(request);
        if (loginIpRateLimiter.tryAcquire(clientKey)) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("error", "Too Many Requests");
        body.put("message", "Trop de tentatives de connexion. Veuillez patienter avant de réessayer.");
        body.put("timestamp", Instant.now().toString());
        objectMapper.writeValue(response.getWriter(), body);
    }

    static boolean isLoginPost(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = pathWithinApplication(request);
        return LOGIN_PATH.equals(path);
    }

    private String resolveClientKey(HttpServletRequest request) {
        if (properties.isTrustForwardedForHeader()) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                String first = xff.split(",")[0].strip();
                if (!first.isEmpty()) {
                    return first;
                }
            }
        }
        String addr = request.getRemoteAddr();
        return addr != null && !addr.isBlank() ? addr : "unknown";
    }

    private static String pathWithinApplication(HttpServletRequest request) {
        String cp = Objects.requireNonNullElse(request.getContextPath(), "");
        String uri = request.getRequestURI();
        if (!uri.startsWith(cp)) {
            return uri.startsWith("/") ? uri : "/" + uri;
        }
        String path = uri.substring(cp.length());
        if (path.isEmpty()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
