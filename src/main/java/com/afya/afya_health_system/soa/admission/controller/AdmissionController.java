package com.afya.afya_health_system.soa.admission.controller;

import com.afya.afya_health_system.soa.admission.dto.*;
import com.afya.afya_health_system.soa.admission.service.AdmissionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST entry points for hospital stays, transfers, discharge/death, length-of-stay and movement history.
 * <p>Français : API des hospitalisations (admission, transfert, sortie, décès, durée de séjour, mouvements).</p>
 */
@RestController
@RequestMapping("/api/v1/admissions")
public class AdmissionController {

    private final AdmissionService admissionService;

    public AdmissionController(AdmissionService admissionService) {
        this.admissionService = admissionService;
    }

    /**
     * Opens a new inpatient stay.
     * <p>Français : ouverture d'une nouvelle admission.</p>
     */
    @PostMapping
    public AdmissionResponse create(@Valid @RequestBody AdmissionCreateRequest request, Authentication authentication) {
        return admissionService.create(request, authentication.getName());
    }

    /**
     * Proposition automatique chambre/lit selon occupation du service et capacité configurée.
     * <p>Français : suggestion de placement pour une admission (évite collision avec {@code /{id}}).</p>
     */
    @GetMapping("/suggestions/bed")
    public BedSuggestionResponse suggestBed(@RequestParam String serviceName, Authentication authentication) {
        return admissionService.suggestBed(serviceName, authentication.getName());
    }

    /**
     * Single admission by id.
     * <p>Français : détail d'une admission.</p>
     */
    @GetMapping("/{id}")
    public AdmissionResponse getById(@PathVariable Long id, Authentication authentication) {
        return admissionService.getById(id, authentication.getName());
    }

    /**
     * Filtered listing (patient and/or status).
     * <p>Français : liste filtrée par patient et/ou statut.</p>
     */
    @GetMapping
    public Page<AdmissionResponse> list(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication
    ) {
        return admissionService.list(patientId, status, sortBy, sortDir, page, size, authentication.getName());
    }

    /**
     * Inter-service or inter-room transfer.
     * <p>Français : transfert de service ou de lit.</p>
     */
    @PutMapping("/{id}/transfer")
    public AdmissionResponse transfer(@PathVariable Long id, @Valid @RequestBody TransferRequest request, Authentication authentication) {
        return admissionService.transfer(id, request, authentication.getName());
    }

    /**
     * Normal discharge.
     * <p>Français : sortie hospitalière standard.</p>
     */
    @PutMapping("/{id}/discharge")
    public AdmissionResponse discharge(
            @PathVariable Long id,
            @RequestBody(required = false) DischargeRequest request,
            Authentication authentication
    ) {
        return admissionService.discharge(id, request == null ? new DischargeRequest(null) : request, authentication.getName());
    }

    /**
     * Death on unit.
     * <p>Français : déclaration de décès pendant le séjour.</p>
     */
    @PutMapping("/{id}/declare-death")
    public AdmissionResponse declareDeath(
            @PathVariable Long id,
            @RequestBody(required = false) DeathDeclarationRequest request,
            Authentication authentication
    ) {
        return admissionService.declareDeath(id, request == null ? new DeathDeclarationRequest(null) : request, authentication.getName());
    }

    /**
     * Calendar-day length of stay snapshot.
     * <p>Français : durée de séjour (jours calendaires).</p>
     */
    @GetMapping("/{id}/length-of-stay")
    public LengthOfStayResponse lengthOfStay(@PathVariable Long id, Authentication authentication) {
        return admissionService.lengthOfStay(id, authentication.getName());
    }

    /**
     * Audit trail of movements for one admission.
     * <p>Français : historique des mouvements pour une admission.</p>
     */
    @GetMapping("/{id}/movements")
    public List<MovementResponse> movements(@PathVariable Long id, Authentication authentication) {
        return admissionService.movements(id, authentication.getName());
    }
}
