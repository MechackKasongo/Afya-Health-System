package com.afya.afya_health_system.soa.urgence.dto;

import jakarta.validation.constraints.Size;

public record CloseRequest(@Size(max = 255) String details) {}
