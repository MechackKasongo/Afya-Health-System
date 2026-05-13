package com.afya.afya_health_system.soa.identity.config;

import com.afya.afya_health_system.soa.identity.service.LoginIpRateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Objects;

/**
 * Stateless HTTP security: JWT filter before form login; login/refresh and health are public.
 * OpenAPI UI and H2 console are handled by a dedicated filter chain ({@link #openapiAndH2Chain}) —
 * permitted only when {@code dev} is active and {@code oracle} is not.
 * Unauthorized API calls return HTTP 401 without redirect.
 * <p>Français : API sans session ; login, refresh et santé publics. Swagger/H2 ont une chaîne dédiée,
 * ouverte uniquement en {@code dev} sans {@code oracle}; sinon refus sur ces chemins.</p>
 */
@Configuration
@EnableConfigurationProperties({
        LoginRateLimitProperties.class,
        PasswordPolicyProperties.class,
        LoginLockoutProperties.class
})
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final Environment environment;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, Environment environment) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.environment = environment;
    }

    /** Local H2/UI only — never expose docs when connecting to Oracle even if {@code dev} is also activated. */
    private boolean openapiAndH2PermittedForCurrentProfiles() {
        return environment.acceptsProfiles(Profiles.of("dev"))
                && !environment.acceptsProfiles(Profiles.of("oracle"));
    }

    /**
     * Matches OpenAPI docs, Swagger UI, and H2 console using {@code requestURI} (minus context path).
     * Reliable with MockMvc and embedded Tomcat; unlike {@link org.springframework.security.web.util.matcher.RegexRequestMatcher},
     * servletPath/pathInfo inconsistencies do not bypass this matcher.
     */
    private static RequestMatcher openapiAndH2Paths() {
        return request -> {
            String contextPath = Objects.requireNonNullElse(request.getContextPath(), "");
            String uri = request.getRequestURI();
            if (!uri.startsWith(contextPath)) {
                return false;
            }
            String path = uri.substring(contextPath.length());
            if (path.isEmpty()) {
                path = "/";
            }
            return path.startsWith("/swagger-ui")
                    || path.startsWith("/v3/api-docs")
                    || "/swagger-ui.html".equals(path)
                    || path.startsWith("/h2-console");
        };
    }

    /**
     * Applies only to OpenAPI docs and H2 UI so behaviour does not depend on MVC handler mapping (MockMvc-safe).
     */
    @Bean
    @Order(0)
    SecurityFilterChain openapiAndH2Chain(HttpSecurity http) throws Exception {
        http.securityMatcher(openapiAndH2Paths());

        http.csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(HttpStatus.FORBIDDEN.value())));

        if (openapiAndH2PermittedForCurrentProfiles()) {
            http.authorizeHttpRequests(a -> a.anyRequest().permitAll());
        } else {
            http.authorizeHttpRequests(a -> a.anyRequest().denyAll());
        }

        return http.build();
    }

    @Bean
    LoginRateLimitFilter loginRateLimitFilter(
            LoginIpRateLimiter loginIpRateLimiter,
            LoginRateLimitProperties loginRateLimitProperties,
            ObjectMapper objectMapper
    ) {
        return new LoginRateLimitFilter(loginIpRateLimiter, loginRateLimitProperties, objectMapper);
    }

    /**
     * Do not attach this throttle to {@code DispatcherServlet}; it only runs inside {@link #apiSecurityFilterChain}.
     * <p>Français : évite l’auto-enregistrement Spring Boot comme filtre servlet global.</p>
     */
    @Bean
    FilterRegistrationBean<LoginRateLimitFilter> loginRateLimitFilterServletSuppress(
            LoginRateLimitFilter loginRateLimitFilter
    ) {
        FilterRegistrationBean<LoginRateLimitFilter> bean = new FilterRegistrationBean<>(loginRateLimitFilter);
        bean.setEnabled(false);
        return bean;
    }

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            LoginRateLimitFilter loginRateLimitFilter
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // User administration
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/hospital-services/**").hasAnyRole("ADMIN", "RECEPTION", "MEDECIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/hospital-services/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/hospital-services/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/hospital-services/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/hospital-services/**").hasRole("ADMIN")

                        // Reporting / exports (admin)
                        .requestMatchers("/api/v1/stats/**", "/api/v1/audit/**", "/api/v1/exports/**").hasRole("ADMIN")

                        // Patients: registration by reception/admin, read by care team
                        .requestMatchers(HttpMethod.GET, "/api/v1/patients/**").hasAnyRole("ADMIN", "MEDECIN", "INFIRMIER", "RECEPTION")
                        .requestMatchers(HttpMethod.POST, "/api/v1/patients/**").hasAnyRole("ADMIN", "RECEPTION")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/patients/**").hasAnyRole("ADMIN", "RECEPTION")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/patients/**").hasAnyRole("ADMIN", "RECEPTION")

                        // Nursing care recording (vital signs)
                        .requestMatchers(HttpMethod.POST, "/api/v1/admissions/*/vital-signs").hasAnyRole("ADMIN", "MEDECIN", "INFIRMIER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/admissions/*/vital-signs/**").hasAnyRole("ADMIN", "MEDECIN", "INFIRMIER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/admissions/*/vital-signs/**").hasAnyRole("ADMIN", "MEDECIN", "INFIRMIER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/admissions/*/vital-signs/**").hasAnyRole("ADMIN", "MEDECIN", "INFIRMIER")

                        // Medical decisions (prescriptions + clinical form): doctor/admin only
                        .requestMatchers(HttpMethod.POST, "/api/v1/admissions/*/prescription-lines/**", "/api/v1/admissions/*/clinical-form").hasAnyRole("ADMIN", "MEDECIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/admissions/*/prescription-lines/**", "/api/v1/admissions/*/clinical-form").hasAnyRole("ADMIN", "MEDECIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/admissions/*/prescription-lines/**", "/api/v1/admissions/*/clinical-form").hasAnyRole("ADMIN", "MEDECIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/admissions/*/prescription-lines/**", "/api/v1/admissions/*/clinical-form").hasAnyRole("ADMIN", "MEDECIN")

                        // Admissions: création aussi par médecin (urgences/consultations) ; mise à jour séjour réservée admin/réception
                        .requestMatchers(HttpMethod.GET, "/api/v1/admissions/**").hasAnyRole("ADMIN", "MEDECIN", "INFIRMIER", "RECEPTION")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admissions/**").hasAnyRole("ADMIN", "RECEPTION", "MEDECIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/admissions/**").hasAnyRole("ADMIN", "RECEPTION")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/admissions/**").hasAnyRole("ADMIN", "RECEPTION")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/admissions/**").hasAnyRole("ADMIN", "RECEPTION")

                        // Medical care by doctors; care recording allowed to nurses where applicable
                        .requestMatchers(HttpMethod.GET, "/api/v1/consultations/**", "/api/v1/urgences/**", "/api/v1/medical-records/**", "/api/v1/patients/*/clinical-timeline")
                        .hasAnyRole("ADMIN", "MEDECIN", "INFIRMIER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/consultations/**").hasAnyRole("ADMIN", "MEDECIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/consultations/**").hasAnyRole("ADMIN", "MEDECIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/consultations/**").hasAnyRole("ADMIN", "MEDECIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/consultations/**").hasAnyRole("ADMIN", "MEDECIN")

                        .requestMatchers(HttpMethod.POST, "/api/v1/urgences/**", "/api/v1/medical-records/**").hasAnyRole("ADMIN", "MEDECIN", "INFIRMIER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/urgences/**", "/api/v1/medical-records/**").hasAnyRole("ADMIN", "MEDECIN", "INFIRMIER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/urgences/**", "/api/v1/medical-records/**").hasAnyRole("ADMIN", "MEDECIN", "INFIRMIER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/urgences/**", "/api/v1/medical-records/**").hasAnyRole("ADMIN", "MEDECIN", "INFIRMIER")

                        // Authenticated profile endpoints
                        .requestMatchers("/api/v1/auth/me", "/api/v1/auth/logout")
                        .hasAnyRole("ADMIN", "MEDECIN", "INFIRMIER", "RECEPTION")

                        .anyRequest()
                        .authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) -> response.sendError(HttpStatus.FORBIDDEN.value()))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(loginRateLimitFilter, JwtAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
