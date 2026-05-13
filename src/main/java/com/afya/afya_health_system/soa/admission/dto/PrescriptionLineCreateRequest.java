package com.afya.afya_health_system.soa.admission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PrescriptionLineCreateRequest(
        @NotBlank @Size(max = 500) String medicationName,
        @Size(max = 500) String dosageText,
        @Size(max = 255) String frequencyText,
        @Size(max = 12000) String instructionsText,
        @Size(max = 120) String prescriberName,
        @NotNull LocalDate startDate,
        LocalDate endDate
) {
}
