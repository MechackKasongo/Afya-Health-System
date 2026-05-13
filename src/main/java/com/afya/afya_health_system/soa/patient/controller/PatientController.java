package com.afya.afya_health_system.soa.patient.controller;

import com.afya.afya_health_system.soa.patient.dto.PatientAdministrativeSummaryResponse;
import com.afya.afya_health_system.soa.patient.dto.PatientContactsUpdateRequest;
import com.afya.afya_health_system.soa.patient.dto.PatientCreateRequest;
import com.afya.afya_health_system.soa.patient.dto.PatientResponse;
import com.afya.afya_health_system.soa.patient.dto.PatientUpdateRequest;
import com.afya.afya_health_system.soa.patient.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST façade for administrative patient CRUD and lightweight search endpoints.
 * <p>Français : API REST pour le registre patient (création, lecture, mise à jour, recherche paginée, résumé admin).</p>
 */
@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    /**
     * Registers a new patient; dossier number must be unique.
     * <p>Français : enregistre un patient ; le numéro de dossier doit être unique.</p>
     */
    @PostMapping
    public PatientResponse create(@Valid @RequestBody PatientCreateRequest request) {
        return patientService.create(request);
    }

    /**
     * Loads one patient by primary key.
     * <p>Français : détail patient par identifiant.</p>
     */
    @GetMapping("/{id}")
    public PatientResponse getById(@PathVariable Long id) {
        return patientService.getById(id);
    }

    /**
     * Paginated listing with optional textual filter on dossier/name fields.
     * <p>Français : liste paginée ; filtre texte facultatif sur nom ou dossier.</p>
     */
    @GetMapping
    public Page<PatientResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return patientService.search(query, page, size, sortBy, sortDir);
    }

    /**
     * Full update of demographics (non-contact fields follow service rules).
     * <p>Français : mise à jour des données administratives du patient.</p>
     */
    @PutMapping("/{id}")
    public PatientResponse update(@PathVariable Long id, @Valid @RequestBody PatientUpdateRequest request) {
        return patientService.update(id, request);
    }

    /**
     * Partial update limited to phone/email/address.
     * <p>Français : mise à jour partielle des coordonnées (téléphone, email, adresse).</p>
     */
    @PatchMapping("/{id}/contacts")
    public PatientResponse updateContacts(@PathVariable Long id, @Valid @RequestBody PatientContactsUpdateRequest request) {
        return patientService.updateContacts(id, request);
    }

    /**
     * Aggregated admin view (admissions, consultations, etc. as implemented in service).
     * <p>Français : vue administrative agrégée pour le patient (selon la logique métier).</p>
     */
    @GetMapping("/{id}/administrative-summary")
    public PatientAdministrativeSummaryResponse administrativeSummary(@PathVariable Long id) {
        return patientService.administrativeSummary(id);
    }
}
