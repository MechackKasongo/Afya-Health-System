package com.afya.afya_health_system.soa.admission.dto;

import jakarta.validation.constraints.Size;

public record DischargeRequest(@Size(max = 255) String note) {}
