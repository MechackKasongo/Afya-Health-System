package com.afya.afya_health_system.soa.admission.dto;

import com.afya.afya_health_system.soa.admission.model.VitalSignSlot;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request body to chart vitals for one admission.
 * <p>Français : saisie des constantes pour une admission ; tous les champs cliniques sont facultatifs sauf la date/heure.</p>
 */
public record VitalSignCreateRequest(
        @NotNull LocalDateTime recordedAt,
        VitalSignSlot slot,
        Integer systolicBp,
        Integer diastolicBp,
        Integer pulseBpm,
        BigDecimal temperatureCelsius,
        BigDecimal weightKg,
        Integer diuresisMl,
        @Size(max = 500) String stoolsNote
) {
}
