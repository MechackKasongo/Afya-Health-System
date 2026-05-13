package com.afya.afya_health_system.soa.consultation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EventCreateRequest(@NotBlank @Size(max = 4000) String content) {}
