package com.afya.afya_health_system.soa.medicalrecord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TextUpdateRequest(@NotBlank @Size(max = 4000) String content) {}
