package com.afya.afya_health_system.soa.admission.dto;

import com.afya.afya_health_system.soa.admission.model.VitalSignSlot;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MedicationAdministrationCreateRequest(
        @NotNull LocalDate administrationDate,
        @NotNull VitalSignSlot slot,
        boolean administered
) {
}
