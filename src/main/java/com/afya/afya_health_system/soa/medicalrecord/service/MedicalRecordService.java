package com.afya.afya_health_system.soa.medicalrecord.service;

import com.afya.afya_health_system.soa.common.exception.ConflictException;
import com.afya.afya_health_system.soa.common.exception.NotFoundException;
import com.afya.afya_health_system.soa.medicalrecord.dto.*;
import com.afya.afya_health_system.soa.medicalrecord.model.MedicalRecord;
import com.afya.afya_health_system.soa.medicalrecord.model.MedicalRecordEntry;
import com.afya.afya_health_system.soa.medicalrecord.repository.MedicalRecordEntryRepository;
import com.afya.afya_health_system.soa.medicalrecord.repository.MedicalRecordRepository;
import com.afya.afya_health_system.soa.patient.model.Patient;
import com.afya.afya_health_system.soa.patient.repository.PatientRepository;
import com.afya.afya_health_system.soa.patient.service.PatientLivingGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * One medical record per patient (allergies/antecedents) plus append-only entries (problems, documents).
 * Record creation is idempotent at domain level: duplicate open record is rejected with conflict.
 * <p>Français : un dossier médical par patient (allergies, antécédents) et lignes append-only (problèmes, pièces).
 * Une tentative de dossier en double pour le même patient est refusée (conflit).</p>
 */
@Service
@Transactional
public class MedicalRecordService {
    private final MedicalRecordRepository recordRepository;
    private final MedicalRecordEntryRepository entryRepository;
    private final PatientRepository patientRepository;
    private final PatientLivingGuard patientLivingGuard;

    public MedicalRecordService(
            MedicalRecordRepository recordRepository,
            MedicalRecordEntryRepository entryRepository,
            PatientRepository patientRepository,
            PatientLivingGuard patientLivingGuard
    ) {
        this.recordRepository = recordRepository;
        this.entryRepository = entryRepository;
        this.patientRepository = patientRepository;
        this.patientLivingGuard = patientLivingGuard;
    }

    public MedicalRecordResponse create(RecordCreateRequest request) {
        ensurePatientExists(request.patientId());
        patientLivingGuard.ensureAlive(request.patientId());
        if (recordRepository.findByPatientId(request.patientId()).isPresent()) {
            throw new ConflictException("Le dossier medical existe deja");
        }
        MedicalRecord r = new MedicalRecord();
        r.setPatientId(request.patientId());
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return toResponse(recordRepository.save(r));
    }

    public MedicalRecordResponse get(Long patientId) {
        return toResponse(findRecord(patientId));
    }

    public MedicalRecordResponse updateAllergies(Long patientId, TextUpdateRequest request) {
        patientLivingGuard.ensureAlive(patientId);
        MedicalRecord r = findRecord(patientId);
        r.setAllergies(request.content());
        r.setUpdatedAt(LocalDateTime.now());
        return toResponse(recordRepository.save(r));
    }

    public MedicalRecordResponse updateAntecedents(Long patientId, TextUpdateRequest request) {
        patientLivingGuard.ensureAlive(patientId);
        MedicalRecord r = findRecord(patientId);
        r.setAntecedents(request.content());
        r.setUpdatedAt(LocalDateTime.now());
        return toResponse(recordRepository.save(r));
    }

    public MedicalRecordEntryResponse addProblem(Long patientId, TextUpdateRequest request) {
        findRecord(patientId);
        patientLivingGuard.ensureAlive(patientId);
        return addEntry(patientId, "PROBLEM", request.content());
    }

    public MedicalRecordEntryResponse addDocument(Long patientId, TextUpdateRequest request) {
        findRecord(patientId);
        patientLivingGuard.ensureAlive(patientId);
        return addEntry(patientId, "DOCUMENT", request.content());
    }

    public MedicalRecordResponse summary(Long patientId) {
        return get(patientId);
    }

    public List<MedicalRecordEntryResponse> history(Long patientId) {
        findRecord(patientId);
        return entryRepository.findByPatientIdOrderByCreatedAtAsc(patientId).stream()
                .map(this::toEntryResponse)
                .toList();
    }

    private void ensurePatientExists(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new NotFoundException("Patient introuvable: " + patientId);
        }
    }

    private MedicalRecord findRecord(Long patientId) {
        return recordRepository.findByPatientId(patientId)
                .orElseThrow(() -> new NotFoundException("Dossier medical introuvable pour patient: " + patientId));
    }

    private MedicalRecordEntryResponse addEntry(Long patientId, String type, String content) {
        MedicalRecordEntry e = new MedicalRecordEntry();
        e.setPatientId(patientId);
        e.setType(type);
        e.setContent(content);
        e.setCreatedAt(LocalDateTime.now());
        return toEntryResponse(entryRepository.save(e));
    }

    private MedicalRecordResponse toResponse(MedicalRecord r) {
        LocalDateTime deceasedAt = patientRepository.findById(r.getPatientId()).map(Patient::getDeceasedAt).orElse(null);
        return new MedicalRecordResponse(
                r.getId(),
                r.getPatientId(),
                r.getAllergies(),
                r.getAntecedents(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                deceasedAt
        );
    }

    private MedicalRecordEntryResponse toEntryResponse(MedicalRecordEntry e) {
        return new MedicalRecordEntryResponse(e.getId(), e.getPatientId(), e.getType(), e.getContent(), e.getCreatedAt());
    }
}
