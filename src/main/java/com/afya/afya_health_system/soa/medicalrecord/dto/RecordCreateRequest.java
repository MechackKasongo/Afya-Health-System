package com.afya.afya_health_system.soa.medicalrecord.dto;

import jakarta.validation.constraints.NotNull;

public record RecordCreateRequest(@NotNull Long patientId) {}
