package com.afya.afya_health_system.soa.patient.service;

import com.afya.afya_health_system.soa.patient.dto.PatientAdministrativeSummaryResponse;
import com.afya.afya_health_system.soa.patient.dto.PatientContactsUpdateRequest;
import com.afya.afya_health_system.soa.patient.dto.PatientCreateRequest;
import com.afya.afya_health_system.soa.patient.dto.PatientResponse;
import com.afya.afya_health_system.soa.patient.dto.PatientUpdateRequest;
import com.afya.afya_health_system.soa.common.exception.ConflictException;
import com.afya.afya_health_system.soa.common.exception.NotFoundException;
import com.afya.afya_health_system.soa.medicalrecord.model.MedicalRecord;
import com.afya.afya_health_system.soa.medicalrecord.repository.MedicalRecordRepository;
import com.afya.afya_health_system.soa.patient.model.PatientDossierSequence;
import com.afya.afya_health_system.soa.patient.model.Patient;
import com.afya.afya_health_system.soa.patient.repository.PatientDossierSequenceRepository;
import com.afya.afya_health_system.soa.patient.repository.PatientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
/**
 * Administrative patient registry: CRUD, search with safe pagination defaults, duplicate dossier guard.
 * <p>Français : registre administratif des patients (CRUD, recherche paginée avec bornes sûres,
 * garde-fou sur le numéro de dossier en double).</p>
 */
@Service
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientDossierSequenceRepository patientDossierSequenceRepository;
    private final PatientLivingGuard patientLivingGuard;

    public PatientService(
            PatientRepository patientRepository,
            MedicalRecordRepository medicalRecordRepository,
            PatientDossierSequenceRepository patientDossierSequenceRepository,
            PatientLivingGuard patientLivingGuard
    ) {
        this.patientRepository = patientRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.patientDossierSequenceRepository = patientDossierSequenceRepository;
        this.patientLivingGuard = patientLivingGuard;
    }

    public PatientResponse create(PatientCreateRequest request) {
        // Check for duplicate patients using key identifying fields
        checkForDuplicatePatient(request);

        String dossierNumber = request.dossierNumber();
        if (dossierNumber != null && !dossierNumber.isBlank()) {
            if (patientRepository.existsByDossierNumber(dossierNumber.trim())) {
                throw new ConflictException("Le numéro de dossier existe déjà");
            }
            dossierNumber = dossierNumber.trim();
        } else {
            dossierNumber = generateUniqueDossierNumber();
        }
        Patient patient = new Patient();
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setDossierNumber(dossierNumber);
        patient.setBirthDate(request.birthDate());
        patient.setSex(request.sex());
        patient.setPhone(request.phone());
        patient.setEmail(request.email());
        patient.setAddress(request.address());
        patient.setPostName(request.postName());
        patient.setEmployer(request.employer());
        patient.setEmployeeId(request.employeeId());
        patient.setProfession(request.profession());
        patient.setSpouseName(request.spouseName());
        patient.setSpouseProfession(request.spouseProfession());
        Patient saved = patientRepository.save(patient);

        // Open the medical record automatically for every new patient.
        // If a folder already exists (shouldn't happen on creation), keep patient creation successful.
        if (medicalRecordRepository.findByPatientId(saved.getId()).isEmpty()) {
            MedicalRecord r = new MedicalRecord();
            r.setPatientId(saved.getId());
            r.setCreatedAt(LocalDateTime.now());
            r.setUpdatedAt(LocalDateTime.now());
            medicalRecordRepository.save(r);
        }

        return toResponse(saved);
    }

    private String generateUniqueDossierNumber() {
        // Format: DOS-YYYY-AAAA-0001 ; séquence strictement incrémentale par année.
        int year = LocalDate.now().getYear();
        PatientDossierSequence sequence = patientDossierSequenceRepository.findByYearForUpdate(year)
                .orElseGet(() -> {
                    PatientDossierSequence created = new PatientDossierSequence();
                    created.setSequenceYear(year);
                    created.setLetterBlock("AAAA");
                    created.setSequenceNumber(0);
                    return created;
                });

        int nextNumber = sequence.getSequenceNumber() + 1;
        String letterBlock = sequence.getLetterBlock();
        if (nextNumber > 9999) {
            nextNumber = 1;
            letterBlock = incrementLetterBlock(letterBlock);
        }

        sequence.setLetterBlock(letterBlock);
        sequence.setSequenceNumber(nextNumber);
        patientDossierSequenceRepository.save(sequence);

        String candidate = "DOS-" + year + "-" + letterBlock + "-" + String.format("%04d", nextNumber);
        if (patientRepository.existsByDossierNumber(candidate)) {
            throw new ConflictException("Collision détectée sur le numéro de dossier: " + candidate);
        }
        return candidate;
    }

    /**
     * Check for duplicate patients using key identifying fields:
     * firstName, lastName, birthDate, sex, postName, and phone.
     * Prevents registration of patients already in the system.
     * <p>Français : vérifie les doublons avec les champs clés identifiants.</p>
     */
    private void checkForDuplicatePatient(PatientCreateRequest request) {
        var duplicates = patientRepository.findDuplicatePatients(
                request.firstName(),
                request.lastName(),
                request.birthDate(),
                request.sex(),
                request.postName() != null ? request.postName() : "",
                request.phone() != null ? request.phone() : ""
        );

        if (!duplicates.isEmpty()) {
            Patient existingPatient = duplicates.getFirst();
            throw new ConflictException(
                    "Un patient avec ces informations existe déjà: " +
                    "Prénom: " + existingPatient.getFirstName() +
                    ", Nom: " + existingPatient.getLastName() +
                    ", Date de naissance: " + existingPatient.getBirthDate() +
                    ", Numéro de dossier: " + existingPatient.getDossierNumber()
            );
        }
    }

    private String incrementLetterBlock(String current) {
        char[] chars = current == null || current.length() != 4 ? "AAAA".toCharArray() : current.toCharArray();
        for (int i = chars.length - 1; i >= 0; i--) {
            if (chars[i] < 'Z') {
                chars[i]++;
                for (int j = i + 1; j < chars.length; j++) {
                    chars[j] = 'A';
                }
                return new String(chars);
            }
        }
        throw new ConflictException("Capacité annuelle atteinte pour la génération des dossiers");
    }

    public PatientResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    /**
     * Full-text style match on name/dossier when {@code query} is set; otherwise returns all (paged).
     * <p>Français : recherche type texte sur nom ou dossier si {@code query} est renseigné ; sinon liste paginée.</p>
     */
    public Page<PatientResponse> search(String query, Integer page, Integer size, String sortBy, String sortDir) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 || size > 500 ? 20 : size;
        Sort sort = buildSafeSort(sortBy, sortDir);
        PageRequest pageable = PageRequest.of(safePage, safeSize, sort);
        if (query == null || query.isBlank()) {
            return patientRepository.findAll(pageable).map(this::toResponse);
        }
        return patientRepository.searchByText(query.trim(), pageable).map(this::toResponse);
    }

    private Sort buildSafeSort(String sortBy, String sortDir) {
        // Whitelist: only allow sorting by known entity fields.
        Set<String> allowed = Set.of("id", "dossierNumber", "firstName", "lastName", "birthDate", "sex");
        String field = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();
        if (!allowed.contains(field)) field = "id";

        boolean desc = sortDir == null || sortDir.isBlank() || sortDir.equalsIgnoreCase("desc");
        Sort.Direction dir = desc ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }

    public PatientResponse update(Long id, PatientUpdateRequest request) {
        Patient patient = findEntity(id);
        patientLivingGuard.ensureAlive(patient);
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setBirthDate(request.birthDate());
        patient.setSex(request.sex());
        patient.setPhone(request.phone());
        patient.setEmail(request.email());
        patient.setAddress(request.address());
        // Optional extended demographics: only overwrite when a value is present (omitted JSON keys stay null and keep DB).
        if (request.postName() != null) {
            patient.setPostName(request.postName());
        }
        if (request.employer() != null) {
            patient.setEmployer(request.employer());
        }
        if (request.employeeId() != null) {
            patient.setEmployeeId(request.employeeId());
        }
        if (request.profession() != null) {
            patient.setProfession(request.profession());
        }
        if (request.spouseName() != null) {
            patient.setSpouseName(request.spouseName());
        }
        if (request.spouseProfession() != null) {
            patient.setSpouseProfession(request.spouseProfession());
        }
        return toResponse(patientRepository.save(patient));
    }

    public PatientResponse updateContacts(Long id, PatientContactsUpdateRequest request) {
        Patient patient = findEntity(id);
        patientLivingGuard.ensureAlive(patient);
        patient.setPhone(request.phone());
        patient.setEmail(request.email());
        patient.setAddress(request.address());
        return toResponse(patientRepository.save(patient));
    }

    public PatientAdministrativeSummaryResponse administrativeSummary(Long id) {
        Patient patient = findEntity(id);
        String fullName = buildFullName(patient);
        return new PatientAdministrativeSummaryResponse(
                patient.getId(),
                fullName,
                patient.getDossierNumber(),
                patient.getSex(),
                patient.getPhone(),
                patient.getEmail(),
                patient.getAddress(),
                patient.getPostName(),
                patient.getEmployer(),
                patient.getEmployeeId(),
                patient.getProfession(),
                patient.getSpouseName(),
                patient.getSpouseProfession()
        );
    }

    private static String buildFullName(Patient patient) {
        StringBuilder sb = new StringBuilder();
        sb.append(patient.getFirstName()).append(' ').append(patient.getLastName());
        if (patient.getPostName() != null && !patient.getPostName().isBlank()) {
            sb.append(' ').append(patient.getPostName());
        }
        return sb.toString();
    }

    private Patient findEntity(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient introuvable: " + id));
    }

    private PatientResponse toResponse(Patient p) {
        return new PatientResponse(
                p.getId(),
                p.getFirstName(),
                p.getLastName(),
                p.getDossierNumber(),
                p.getBirthDate(),
                p.getSex(),
                p.getPhone(),
                p.getEmail(),
                p.getAddress(),
                p.getPostName(),
                p.getEmployer(),
                p.getEmployeeId(),
                p.getProfession(),
                p.getSpouseName(),
                p.getSpouseProfession(),
                p.getDeceasedAt()
        );
    }
}
