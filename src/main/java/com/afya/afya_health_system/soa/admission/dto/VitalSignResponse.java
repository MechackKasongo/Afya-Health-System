package com.afya.afya_health_system.soa.admission.dto;

import com.afya.afya_health_system.soa.admission.model.VitalSignSlot;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VitalSignResponse(
        Long id,
        Long admissionId,
        LocalDateTime recordedAt,
        VitalSignSlot slot,
        Integer systolicBp,
        Integer diastolicBp,
        Integer pulseBpm,
        BigDecimal temperatureCelsius,
        BigDecimal weightKg,
        Integer diuresisMl,
        String stoolsNote
) {
}
