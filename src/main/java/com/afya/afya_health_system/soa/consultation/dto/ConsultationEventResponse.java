package com.afya.afya_health_system.soa.consultation.dto;

import java.time.LocalDateTime;

public record ConsultationEventResponse(
        Long id, Long consultationId, Long patientId, String type, String content, LocalDateTime createdAt
) {}
