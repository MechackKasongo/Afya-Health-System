package com.afya.afya_health_system.soa.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata for springdoc (Swagger UI + {@code /v3/api-docs}).
 * <p>Français : métadonnées de la doc API ; les routes protégées nécessitent un JWT via « Authorize » (Bearer).</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI afyaOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token from POST /api/v1/auth/login (use Authorize in Swagger UI).")))
                .info(new Info()
                        .title("Afya Health System API")
                        .version("1.0")
                        .description("""
                                API REST préfixée par `/api/v1/`.
                                Authentification : JWT Bearer (obtenu via POST `/api/v1/auth/login`).
                                Dans Swagger UI : « Authorize » → schéma bearer-jwt → collez le token (sans le préfixe Bearer si l’UI l’ajoute).
                                """));
    }
}
