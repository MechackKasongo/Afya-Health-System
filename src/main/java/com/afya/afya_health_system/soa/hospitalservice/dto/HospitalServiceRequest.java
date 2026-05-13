package com.afya.afya_health_system.soa.hospitalservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;

public record HospitalServiceRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @Positive Integer bedCapacity
) {}
