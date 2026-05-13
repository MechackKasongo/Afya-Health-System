package com.afya.afya_health_system.soa.admission.dto;

import java.time.LocalDate;

public record LengthOfStayResponse(
        Long admissionId,
        LocalDate admissionDate,
        LocalDate dischargeDate,
        long numberOfDays
) {}
