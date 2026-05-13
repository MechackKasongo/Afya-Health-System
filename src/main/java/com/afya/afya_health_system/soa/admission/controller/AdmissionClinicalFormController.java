package com.afya.afya_health_system.soa.admission.controller;

import com.afya.afya_health_system.soa.admission.dto.AdmissionClinicalFormResponse;
import com.afya.afya_health_system.soa.admission.dto.AdmissionClinicalFormUpsertRequest;
import com.afya.afya_health_system.soa.admission.service.AdmissionClinicalFormService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Structured hospitalization form (history + segmented physical exam + paraclinics + conclusion).
 * <p>Français : formulaire d'hospitalisation structuré par zones de texte pour une admission.</p>
 */
@RestController
@RequestMapping("/api/v1/admissions/{admissionId}/clinical-form")
public class AdmissionClinicalFormController {

    private final AdmissionClinicalFormService admissionClinicalFormService;

    public AdmissionClinicalFormController(AdmissionClinicalFormService admissionClinicalFormService) {
        this.admissionClinicalFormService = admissionClinicalFormService;
    }

    @GetMapping
    public AdmissionClinicalFormResponse get(@PathVariable Long admissionId, Authentication authentication) {
        return admissionClinicalFormService.get(admissionId, authentication.getName());
    }

    @PutMapping
    public AdmissionClinicalFormResponse upsert(
            @PathVariable Long admissionId,
            @Valid @RequestBody AdmissionClinicalFormUpsertRequest request,
            Authentication authentication
    ) {
        return admissionClinicalFormService.upsert(admissionId, request, authentication.getName());
    }
}
