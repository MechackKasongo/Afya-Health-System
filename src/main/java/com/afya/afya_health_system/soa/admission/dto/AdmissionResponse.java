package com.afya.afya_health_system.soa.admission.dto;

import com.afya.afya_health_system.soa.admission.model.AdmissionStatus;

import java.time.LocalDateTime;

public record AdmissionResponse(
        Long id,
        Long patientId,
        String serviceName,
        String room,
        String bed,
        String reason,
        LocalDateTime admissionDateTime,
        LocalDateTime dischargeDateTime,
        AdmissionStatus status
) {}
