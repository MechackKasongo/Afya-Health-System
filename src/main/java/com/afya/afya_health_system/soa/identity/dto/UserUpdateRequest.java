package com.afya.afya_health_system.soa.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserUpdateRequest(
        @NotBlank @Size(max = 120) String fullName,
        @Email @Size(max = 160) String email,
        @NotBlank @Size(max = 80) String role,
        @Size(min = 11, max = 200) String password,
        /** {@code null} = ne pas modifier l'affectation ; liste vide = tout retirer. */
        List<Long> hospitalServiceIds
) {}
