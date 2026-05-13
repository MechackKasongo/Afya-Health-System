package com.afya.afya_health_system.soa.hospitalservice.controller;

import com.afya.afya_health_system.soa.hospitalservice.dto.HospitalServiceRequest;
import com.afya.afya_health_system.soa.hospitalservice.dto.HospitalServiceResponse;
import com.afya.afya_health_system.soa.hospitalservice.dto.HospitalServiceStatusRequest;
import com.afya.afya_health_system.soa.hospitalservice.service.HospitalServiceManagementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hospital-services")
public class HospitalServiceManagementController {
    private final HospitalServiceManagementService hospitalServiceManagementService;

    public HospitalServiceManagementController(HospitalServiceManagementService hospitalServiceManagementService) {
        this.hospitalServiceManagementService = hospitalServiceManagementService;
    }

    @GetMapping
    public Page<HospitalServiceResponse> list(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return hospitalServiceManagementService.list(activeOnly, page, size);
    }

    @PostMapping
    public HospitalServiceResponse create(@Valid @RequestBody HospitalServiceRequest request) {
        return hospitalServiceManagementService.create(request);
    }

    @PutMapping("/{id}")
    public HospitalServiceResponse update(@PathVariable Long id, @Valid @RequestBody HospitalServiceRequest request) {
        return hospitalServiceManagementService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public HospitalServiceResponse updateStatus(@PathVariable Long id, @RequestBody HospitalServiceStatusRequest request) {
        return hospitalServiceManagementService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        hospitalServiceManagementService.delete(id);
    }
}
