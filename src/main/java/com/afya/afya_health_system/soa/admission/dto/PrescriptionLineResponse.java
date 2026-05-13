package com.afya.afya_health_system.soa.admission.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PrescriptionLineResponse(
        Long id,
        Long admissionId,
        String medicationName,
        String dosageText,
        String frequencyText,
        String instructionsText,
        String prescriberName,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        LocalDateTime createdAt
) {
}
