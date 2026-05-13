package com.afya.afya_health_system.soa.patient.dto;

public record PatientAdministrativeSummaryResponse(
        Long id,
        String fullName,
        String dossierNumber,
        String sex,
        String phone,
        String email,
        String address,
        String postName,
        String employer,
        String employeeId,
        String profession,
        String spouseName,
        String spouseProfession
) {
}
