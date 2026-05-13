package com.afya.afya_health_system.soa.urgence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UrgenceCreateRequest(
        @NotNull Long patientId,
        @Size(max = 255) String motif,
        @NotBlank @Size(max = 40) String priority
) {}
