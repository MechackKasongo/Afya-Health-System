package com.afya.afya_health_system.soa.urgence.dto;

import com.afya.afya_health_system.soa.urgence.model.UrgenceStatus;

import java.time.LocalDateTime;

public record UrgenceResponse(
        Long id,
        Long patientId,
        String motif,
        String priority,
        String triageLevel,
        String orientation,
        UrgenceStatus status,
        LocalDateTime createdAt,
        LocalDateTime closedAt
) {}
