package com.afya.afya_health_system.soa.admission.dto;

import com.afya.afya_health_system.soa.admission.model.VitalSignSlot;

import java.time.LocalDate;

public record MedicationAdministrationResponse(
        Long id,
        Long prescriptionLineId,
        LocalDate administrationDate,
        VitalSignSlot slot,
        boolean administered
) {
}
