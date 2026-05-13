package com.afya.afya_health_system.soa.consultation.service;

import com.afya.afya_health_system.soa.admission.model.Admission;
import com.afya.afya_health_system.soa.admission.repository.AdmissionRepository;
import com.afya.afya_health_system.soa.common.exception.ConflictException;
import com.afya.afya_health_system.soa.common.exception.NotFoundException;
import com.afya.afya_health_system.soa.consultation.dto.*;
import com.afya.afya_health_system.soa.consultation.model.Consultation;
import com.afya.afya_health_system.soa.consultation.model.ConsultationEvent;
import com.afya.afya_health_system.soa.consultation.repository.ConsultationEventRepository;
import com.afya.afya_health_system.soa.consultation.repository.ConsultationRepository;
import com.afya.afya_health_system.soa.patient.repository.PatientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Consultation header plus append-only clinical events. Every consultation is bound to an admission
 * that belongs to the same patient (enforces care context and prevents cross-patient linkage).
 * <p>Français : en-tête de consultation et événements cliniques en append-only. Chaque consultation est liée
 * à une admission du même patient (contexte de soins, pas de mélange entre patients).</p>
 */
@Service
@Transactional
public class ConsultationService {
    private final ConsultationRepository consultationRepository;
    private final ConsultationEventRepository eventRepository;
    private final PatientRepository patientRepository;
    private final AdmissionRepository admissionRepository;

    public ConsultationService(
            ConsultationRepository consultationRepository,
            ConsultationEventRepository eventRepository,
            PatientRepository patientRepository,
            AdmissionRepository admissionRepository
    ) {
        this.consultationRepository = consultationRepository;
        this.eventRepository = eventRepository;
        this.patientRepository = patientRepository;
        this.admissionRepository = admissionRepository;
    }
    /**
     * Creates header after patient + admission coherence check.
     * <p>Français : crée l'en-tête après cohérence patient + admission.</p>
     */
    public ConsultationResponse create(ConsultationCreateRequest request) {
        ensurePatientExists(request.patientId());
        // Must match same patient / La consultation doit correspondre à une admission du même patient.
        ensureAdmissionBelongsToPatient(request.admissionId(), request.patientId());
        Consultation c = new Consultation();
        c.setPatientId(request.patientId()); c.setAdmissionId(request.admissionId()); c.setDoctorName(request.doctorName()); c.setReason(request.reason()); c.setConsultationDateTime(LocalDateTime.now());
        return toResponse(consultationRepository.save(c));
    }
    public ConsultationResponse getById(Long id) { return toResponse(findConsultation(id)); }
    /**
     * Filters by patient, admission, both, or neither (full list — use with care in production).
     * <p>Français : filtres optionnels ; liste complète si aucun filtre (à utiliser avec prudence en prod).</p>
     */
    public Page<ConsultationResponse> list(Long patientId, Long admissionId, String sortBy, String sortDir, Integer page, Integer size) {
        Sort sort = buildSafeSort(sortBy, sortDir);
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 || size > 500 ? 15 : size;
        PageRequest pageable = PageRequest.of(safePage, safeSize, sort);
        Page<Consultation> entityPage;
        if (patientId != null && admissionId != null) {
            entityPage = consultationRepository.findByPatientIdAndAdmissionId(patientId, admissionId, pageable);
        } else if (patientId != null) {
            entityPage = consultationRepository.findByPatientId(patientId, pageable);
        } else if (admissionId != null) {
            entityPage = consultationRepository.findByAdmissionId(admissionId, pageable);
        } else {
            entityPage = consultationRepository.findAll(pageable);
        }
        return entityPage.map(this::toResponse);
    }

    private Sort buildSafeSort(String sortBy, String sortDir) {
        Set<String> allowed = Set.of("id", "patientId", "admissionId", "doctorName", "consultationDateTime");
        String field = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();
        if (!allowed.contains(field)) field = "id";

        boolean desc = sortDir == null || sortDir.isBlank() || sortDir.equalsIgnoreCase("desc");
        Sort.Direction dir = desc ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }
    public ConsultationEventResponse addObservation(Long consultationId, EventCreateRequest request) { return saveEvent(findConsultation(consultationId), "OBSERVATION", request.content()); }
    public ConsultationEventResponse addDiagnostic(Long consultationId, EventCreateRequest request) { return saveEvent(findConsultation(consultationId), "DIAGNOSTIC", request.content()); }
    public ConsultationEventResponse addExamOrder(Long consultationId, EventCreateRequest request) { return saveEvent(findConsultation(consultationId), "EXAM_ORDER", request.content()); }
    /**
     * All events for the patient across consultations, chronological.
     * <p>Français : tous les événements du patient, toutes consultations confondues, par ordre chronologique.</p>
     */
    public List<ConsultationEventResponse> clinicalTimeline(Long patientId) { return eventRepository.findByPatientIdOrderByCreatedAtAsc(patientId).stream().map(this::toEventResponse).toList(); }

    /**
     * Typed row in {@code consultation_events}; denormalizes patientId for cross-consult queries.
     * <p>Français : ligne typée dans {@code consultation_events} ; {@code patientId} dupliqué pour requêtes transverses.</p>
     */
    private ConsultationEventResponse saveEvent(Consultation consultation, String type, String content) {
        ConsultationEvent e = new ConsultationEvent();
        e.setConsultationId(consultation.getId()); e.setPatientId(consultation.getPatientId()); e.setType(type); e.setContent(content); e.setCreatedAt(LocalDateTime.now());
        return toEventResponse(eventRepository.save(e));
    }
    private Consultation findConsultation(Long id) { return consultationRepository.findById(id).orElseThrow(() -> new NotFoundException("Consultation introuvable: " + id)); }
    private void ensurePatientExists(Long patientId) {
        if (!patientRepository.existsById(patientId)) throw new NotFoundException("Patient introuvable: " + patientId);
    }

    /**
     * Admission must exist and reference the same patient as the consultation payload.
     * <p>Français : l'admission doit exister et concerner le même patient que la demande de consultation.</p>
     */
    private void ensureAdmissionBelongsToPatient(Long admissionId, Long patientId) {
        Admission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new NotFoundException("Admission introuvable: " + admissionId));
        if (!patientId.equals(admission.getPatientId())) {
            throw new ConflictException("L'admission ne correspond pas au patient indiqué");
        }
    }
    private ConsultationResponse toResponse(Consultation c) { return new ConsultationResponse(c.getId(), c.getPatientId(), c.getAdmissionId(), c.getDoctorName(), c.getReason(), c.getConsultationDateTime()); }
    private ConsultationEventResponse toEventResponse(ConsultationEvent e) { return new ConsultationEventResponse(e.getId(), e.getConsultationId(), e.getPatientId(), e.getType(), e.getContent(), e.getCreatedAt()); }
}
