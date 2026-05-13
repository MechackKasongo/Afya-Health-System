package com.afya.afya_health_system.soa.medicalrecord.dto;

import java.time.LocalDateTime;

public record MedicalRecordEntryResponse(
        Long id, Long patientId, String type, String content, LocalDateTime createdAt
) {}
