package com.afya.afya_health_system.soa.identity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        Set<String> roles,
        boolean active,
        List<Long> hospitalServiceIds,
        /** Plain password returned only once on create (never stored). */
        String generatedPassword
) {
    public UserResponse(Long id, String username, String email, String fullName, Set<String> roles, boolean active) {
        this(id, username, email, fullName, roles, active, List.of(), null);
    }

    public UserResponse(Long id, String username, String email, String fullName, Set<String> roles, boolean active, String generatedPassword) {
        this(id, username, email, fullName, roles, active, List.of(), generatedPassword);
    }
}
