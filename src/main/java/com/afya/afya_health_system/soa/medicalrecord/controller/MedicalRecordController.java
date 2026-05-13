package com.afya.afya_health_system.soa.medicalrecord.controller;

import com.afya.afya_health_system.soa.medicalrecord.dto.*;
import com.afya.afya_health_system.soa.medicalrecord.service.MedicalRecordService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Medical record shell and append-only clinical lines keyed by {@code patientId}.
 * <p>Français : dossier médical par patient (allergies, antécédents, problèmes, documents, historique).</p>
 */
@RestController
@RequestMapping("/api/v1/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService service;

    public MedicalRecordController(MedicalRecordService service) {
        this.service = service;
    }

    /**
     * First folder for the patient.
     * <p>Français : ouverture initiale du dossier.</p>
     */
    @PostMapping
    public MedicalRecordResponse create(@Valid @RequestBody RecordCreateRequest request) {
        return service.create(request);
    }

    /**
     * Core allergies/antecedents block.
     * <p>Français : allergies et antécédents agrégés.</p>
     */
    @GetMapping("/{patientId}")
    public MedicalRecordResponse get(@PathVariable Long patientId) {
        return service.get(patientId);
    }

    /**
     * Replace allergies blob.
     * <p>Français : mise à jour du bloc allergies.</p>
     */
    @PutMapping("/{patientId}/allergies")
    public MedicalRecordResponse allergies(@PathVariable Long patientId, @Valid @RequestBody TextUpdateRequest request) {
        return service.updateAllergies(patientId, request);
    }

    /**
     * Replace antecedents blob.
     * <p>Français : mise à jour du bloc antécédents.</p>
     */
    @PutMapping("/{patientId}/antecedents")
    public MedicalRecordResponse antecedents(@PathVariable Long patientId, @Valid @RequestBody TextUpdateRequest request) {
        return service.updateAntecedents(patientId, request);
    }

    /**
     * Append coded problem row.
     * <p>Français : ajout d'une ligne problème.</p>
     */
    @PostMapping("/{patientId}/problems")
    public MedicalRecordEntryResponse problem(@PathVariable Long patientId, @Valid @RequestBody TextUpdateRequest request) {
        return service.addProblem(patientId, request);
    }

    /**
     * Append document reference row.
     * <p>Français : ajout d'une pièce ou note documentaire.</p>
     */
    @PostMapping("/{patientId}/documents")
    public MedicalRecordEntryResponse document(@PathVariable Long patientId, @Valid @RequestBody TextUpdateRequest request) {
        return service.addDocument(patientId, request);
    }

    /**
     * Same payload as GET (explicit summary route).
     * <p>Français : résumé (= lecture dossier).</p>
     */
    @GetMapping("/{patientId}/summary")
    public MedicalRecordResponse summary(@PathVariable Long patientId) {
        return service.summary(patientId);
    }

    /**
     * Chronological free-text rows.
     * <p>Français : historique des entrées problème/document.</p>
     */
    @GetMapping("/{patientId}/history")
    public List<MedicalRecordEntryResponse> history(@PathVariable Long patientId) {
        return service.history(patientId);
    }
}
