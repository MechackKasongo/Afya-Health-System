package com.afya.afya_health_system.soa.patient.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PatientUpdateRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @NotNull LocalDate birthDate,
        @NotBlank @Size(max = 10) String sex,
        @Size(max = 120) String phone,
        @Email @Size(max = 120) String email,
        @Size(max = 255) String address,
        @Size(max = 120) String postName,
        @Size(max = 120) String employer,
        @Size(max = 80) String employeeId,
        @Size(max = 120) String profession,
        @Size(max = 120) String spouseName,
        @Size(max = 120) String spouseProfession
) {
}
