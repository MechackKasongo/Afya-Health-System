package com.afya.afya_health_system.soa.urgence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrientRequest(
        @NotBlank @Size(max = 80) String orientation,
        @Size(max = 255) String details
) {}
