package com.afya.afya_health_system.soa.admission.controller;

import com.afya.afya_health_system.soa.admission.dto.MedicationAdministrationCreateRequest;
import com.afya.afya_health_system.soa.admission.dto.MedicationAdministrationResponse;
import com.afya.afya_health_system.soa.admission.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Administration ticks (date × slot) for one prescription line within an admission.
 * <p>Français : coches d'administration (date et créneau matin/soir) pour une ligne de prescription.</p>
 */
@RestController
@RequestMapping("/api/v1/admissions/{admissionId}/prescription-lines/{lineId}/administrations")
public class MedicationAdministrationController {

    private final PrescriptionService prescriptionService;

    public MedicationAdministrationController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping
    public List<MedicationAdministrationResponse> list(
            @PathVariable Long admissionId,
            @PathVariable Long lineId,
            Authentication authentication
    ) {
        return prescriptionService.listAdministrations(admissionId, lineId, authentication.getName());
    }

    @PostMapping
    public MedicationAdministrationResponse record(
            @PathVariable Long admissionId,
            @PathVariable Long lineId,
            @Valid @RequestBody MedicationAdministrationCreateRequest request,
            Authentication authentication
    ) {
        return prescriptionService.recordAdministration(admissionId, lineId, request, authentication.getName());
    }
}
