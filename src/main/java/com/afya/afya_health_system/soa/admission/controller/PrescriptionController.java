package com.afya.afya_health_system.soa.admission.controller;

import com.afya.afya_health_system.soa.admission.dto.PrescriptionLineCreateRequest;
import com.afya.afya_health_system.soa.admission.dto.PrescriptionLineResponse;
import com.afya.afya_health_system.soa.admission.dto.PrescriptionLineUpdateRequest;
import com.afya.afya_health_system.soa.admission.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Prescription lines scoped to an inpatient admission.
 * <p>Français : prescriptions médicamenteuses pour une admission donnée.</p>
 */
@RestController
@RequestMapping("/api/v1/admissions/{admissionId}/prescription-lines")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping
    public List<PrescriptionLineResponse> list(@PathVariable Long admissionId, Authentication authentication) {
        return prescriptionService.list(admissionId, authentication.getName());
    }

    @PostMapping
    public PrescriptionLineResponse create(
            @PathVariable Long admissionId,
            @Valid @RequestBody PrescriptionLineCreateRequest request,
            Authentication authentication
    ) {
        return prescriptionService.create(admissionId, request, authentication.getName());
    }

    @PutMapping("/{lineId}")
    public PrescriptionLineResponse update(
            @PathVariable Long admissionId,
            @PathVariable Long lineId,
            @Valid @RequestBody PrescriptionLineUpdateRequest request,
            Authentication authentication
    ) {
        return prescriptionService.update(admissionId, lineId, request, authentication.getName());
    }
}
