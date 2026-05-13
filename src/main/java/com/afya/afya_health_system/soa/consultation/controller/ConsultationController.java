package com.afya.afya_health_system.soa.consultation.controller;

import com.afya.afya_health_system.soa.consultation.dto.*;
import com.afya.afya_health_system.soa.consultation.service.ConsultationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Consultation resources under {@code /api/v1}; includes per-patient clinical timeline.
 * <p>Français : consultations et fil chronologique clinique par patient (chemins {@code /consultations} et {@code /patients/.../clinical-timeline}).</p>
 */
@RestController
@RequestMapping("/api/v1")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    /**
     * New consultation bound to patient + admission.
     * <p>Français : nouvelle consultation liée patient + admission.</p>
     */
    @PostMapping("/consultations")
    public ConsultationResponse create(@Valid @RequestBody ConsultationCreateRequest request) {
        return consultationService.create(request);
    }

    /**
     * Consultation header.
     * <p>Français : en-tête de consultation.</p>
     */
    @GetMapping("/consultations/{id}")
    public ConsultationResponse getById(@PathVariable Long id) {
        return consultationService.getById(id);
    }

    /**
     * Optional filters.
     * <p>Français : liste avec filtres patient / admission.</p>
     */
    @GetMapping("/consultations")
    public Page<ConsultationResponse> list(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long admissionId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return consultationService.list(patientId, admissionId, sortBy, sortDir, page, size);
    }

    /**
     * Append observation event.
     * <p>Français : ajout d'une observation clinique.</p>
     */
    @PostMapping("/consultations/{id}/observations")
    public ConsultationEventResponse addObservation(@PathVariable Long id, @Valid @RequestBody EventCreateRequest request) {
        return consultationService.addObservation(id, request);
    }

    /**
     * Append diagnostic event.
     * <p>Français : ajout d'un diagnostic.</p>
     */
    @PostMapping("/consultations/{id}/diagnostics")
    public ConsultationEventResponse addDiagnostic(@PathVariable Long id, @Valid @RequestBody EventCreateRequest request) {
        return consultationService.addDiagnostic(id, request);
    }

    /**
     * Append exam order.
     * <p>Français : prescription d'examen.</p>
     */
    @PostMapping("/consultations/{id}/orders/exams")
    public ConsultationEventResponse addExamOrder(@PathVariable Long id, @Valid @RequestBody EventCreateRequest request) {
        return consultationService.addExamOrder(id, request);
    }

    /**
     * All clinical events for the patient.
     * <p>Français : chronologie clinique du patient.</p>
     */
    @GetMapping("/patients/{patientId}/clinical-timeline")
    public List<ConsultationEventResponse> clinicalTimeline(@PathVariable Long patientId) {
        return consultationService.clinicalTimeline(patientId);
    }
}
