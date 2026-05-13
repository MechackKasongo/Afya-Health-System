package com.afya.afya_health_system.soa.consultation.dto;

import java.time.LocalDateTime;

public record ConsultationResponse(
        Long id, Long patientId, Long admissionId, String doctorName, String reason, LocalDateTime consultationDateTime
) {}
