package com.afya.afya_health_system.soa.patient.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatientResponse(
        Long id,
        String firstName,
        String lastName,
        String dossierNumber,
        LocalDate birthDate,
        String sex,
        String phone,
        String email,
        String address,
        String postName,
        String employer,
        String employeeId,
        String profession,
        String spouseName,
        String spouseProfession,
        LocalDateTime deceasedAt
) {
}
