package com.afya.afya_health_system.soa.admission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdmissionCreateRequest(
        @NotNull Long patientId,
        @NotBlank @Size(max = 80) String serviceName,
        @Size(max = 20) String room,
        @Size(max = 20) String bed,
        @Size(max = 255) String reason
) {}
