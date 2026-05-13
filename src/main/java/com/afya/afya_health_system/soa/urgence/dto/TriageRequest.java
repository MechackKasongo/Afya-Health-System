package com.afya.afya_health_system.soa.urgence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TriageRequest(
        @NotBlank @Size(max = 40) String triageLevel,
        @Size(max = 255) String details
) {}
