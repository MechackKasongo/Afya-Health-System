package com.afya.afya_health_system.soa.medicalrecord.dto;

import java.time.LocalDateTime;

public record MedicalRecordResponse(
        Long id,
        Long patientId,
        String allergies,
        String antecedents,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime patientDeceasedAt
) {}
