package com.afya.afya_health_system.soa.admission.dto;

import java.time.LocalDateTime;

public record MovementResponse(
        Long id,
        Long admissionId,
        String type,
        String fromService,
        String toService,
        LocalDateTime createdAt,
        String note
) {}
