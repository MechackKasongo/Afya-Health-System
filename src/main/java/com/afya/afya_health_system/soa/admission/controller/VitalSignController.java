package com.afya.afya_health_system.soa.admission.controller;

import com.afya.afya_health_system.soa.admission.dto.VitalSignCreateRequest;
import com.afya.afya_health_system.soa.admission.dto.VitalSignResponse;
import com.afya.afya_health_system.soa.admission.service.VitalSignService;
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
 * Vital-sign charting nested under an admission (French inpatient surveillance sheet equivalent).
 * <p>Français : saisie et consultation des constantes pour une admission donnée.</p>
 */
@RestController
@RequestMapping("/api/v1/admissions/{admissionId}/vital-signs")
public class VitalSignController {

    private final VitalSignService vitalSignService;

    public VitalSignController(VitalSignService vitalSignService) {
        this.vitalSignService = vitalSignService;
    }

    /**
     * Lists readings newest-first for one stay.
     * <p>Français : liste des relevés (plus récent en premier) pour un séjour.</p>
     */
    @GetMapping
    public List<VitalSignResponse> list(@PathVariable Long admissionId, Authentication authentication) {
        return vitalSignService.listByAdmission(admissionId, authentication.getName());
    }

    /**
     * Adds one chart row (TA, pulse, temperature, weight, diuresis, stools, optional morning/evening slot).
     * <p>Français : ajoute une ligne de surveillance (TA, pouls, T°, poids, diurèse, selles ; créneau matin/soir facultatif).</p>
     */
    @PostMapping
    public VitalSignResponse create(
            @PathVariable Long admissionId,
            @Valid @RequestBody VitalSignCreateRequest request,
            Authentication authentication
    ) {
        return vitalSignService.create(admissionId, request, authentication.getName());
    }
}
