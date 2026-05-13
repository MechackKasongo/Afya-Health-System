package com.afya.afya_health_system.soa.hospitalservice.dto;

public record HospitalServiceResponse(
        Long id,
        String name,
        Integer bedCapacity,
        boolean active
) {}
