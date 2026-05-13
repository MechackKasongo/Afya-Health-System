package com.afya.afya_health_system.soa.urgence.dto;

import java.time.LocalDateTime;

public record UrgenceTimelineEventResponse(
        Long id,
        Long urgenceId,
        String type,
        String details,
        LocalDateTime createdAt
) {}
