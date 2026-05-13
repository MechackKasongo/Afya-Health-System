package com.afya.afya_health_system.soa.consultation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConsultationCreateRequest(
        @NotNull Long patientId,
        @NotNull Long admissionId,
        @NotBlank @Size(max = 80) String doctorName,
        @Size(max = 255) String reason
) {}
