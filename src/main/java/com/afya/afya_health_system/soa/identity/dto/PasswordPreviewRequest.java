package com.afya.afya_health_system.soa.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordPreviewRequest(
        @NotBlank @Size(max = 60) String firstName,
        @NotBlank @Size(max = 60) String lastName,
        @Size(max = 60) String postName,
        Integer generatedPasswordLength,
        /** Incrémenter pour obtenir une autre combinaison (casse, ordre). */
        Integer variation
) {}
