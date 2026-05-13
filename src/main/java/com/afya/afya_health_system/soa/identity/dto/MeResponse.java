package com.afya.afya_health_system.soa.identity.dto;

import java.util.List;
import java.util.Set;

public record MeResponse(
        Long id,
        String username,
        String fullName,
        Set<String> roles,
        List<Long> hospitalServiceIds,
        List<String> hospitalServiceNames
) {
}
