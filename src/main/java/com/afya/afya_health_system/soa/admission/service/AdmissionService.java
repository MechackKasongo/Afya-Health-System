package com.afya.afya_health_system.soa.admission.service;

import com.afya.afya_health_system.soa.admission.dto.*;
import com.afya.afya_health_system.soa.admission.model.Admission;
import com.afya.afya_health_system.soa.admission.model.AdmissionStatus;
import com.afya.afya_health_system.soa.admission.model.Movement;
import com.afya.afya_health_system.soa.admission.repository.AdmissionRepository;
import com.afya.afya_health_system.soa.admission.repository.MovementRepository;
import com.afya.afya_health_system.soa.common.exception.ConflictException;
import com.afya.afya_health_system.soa.common.exception.NotFoundException;
import com.afya.afya_health_system.soa.hospitalservice.repository.HospitalServiceRepository;
import com.afya.afya_health_system.soa.patient.model.Patient;
import com.afya.afya_health_system.soa.patient.repository.PatientRepository;
import com.afya.afya_health_system.soa.patient.service.PatientLivingGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Inpatient stay workflow: open admission, transfer between services, discharge, death declaration.
 * Each state-changing action appends an audit row in {@code admission_movements}.
 * <p>Français : parcours d'hospitalisation (ouverture, transfert entre services, sortie, décès).
 * Chaque changement d'état ajoute une ligne d'audit dans {@code admission_movements}.</p>
 */
@Service
@Transactional
public class AdmissionService {
    private final AdmissionRepository admissionRepository;
    private final MovementRepository movementRepository;
    private final LengthOfStayCalculatorService calculatorService;
    private final PatientRepository patientRepository;
    private final HospitalServiceRepository hospitalServiceRepository;
    private final BedAllocationService bedAllocationService;
    private final PatientLivingGuard patientLivingGuard;
    private final UserHospitalScopeService userHospitalScopeService;

    public AdmissionService(
            AdmissionRepository admissionRepository,
            MovementRepository movementRepository,
            LengthOfStayCalculatorService calculatorService,
            PatientRepository patientRepository,
            HospitalServiceRepository hospitalServiceRepository,
            BedAllocationService bedAllocationService,
            PatientLivingGuard patientLivingGuard,
            UserHospitalScopeService userHospitalScopeService
    ) {
        this.admissionRepository = admissionRepository;
        this.movementRepository = movementRepository;
        this.calculatorService = calculatorService;
        this.patientRepository = patientRepository;
        this.hospitalServiceRepository = hospitalServiceRepository;
        this.bedAllocationService = bedAllocationService;
        this.patientLivingGuard = patientLivingGuard;
        this.userHospitalScopeService = userHospitalScopeService;
    }

    /**
     * Opens a new stay in {@code EN_COURS} and records an ADMISSION movement.
     * <p>Français : ouvre un séjour {@code EN_COURS} et trace un mouvement ADMISSION.</p>
     */
    public AdmissionResponse create(AdmissionCreateRequest request, String username) {
        ensurePatientExists(request.patientId());
        patientLivingGuard.ensureAlive(request.patientId());
        ensureActiveHospitalService(request.serviceName());
        userHospitalScopeService.assertCanUseServiceName(username, request.serviceName());
        Admission admission = new Admission();
        admission.setPatientId(request.patientId());
        admission.setServiceName(request.serviceName());
        boolean roomBlank = request.room() == null || request.room().isBlank();
        boolean bedBlank = request.bed() == null || request.bed().isBlank();
        if (roomBlank && bedBlank) {
            var suggestion = bedAllocationService.suggest(request.serviceName());
            if (Boolean.TRUE.equals(suggestion.available())
                    && suggestion.room() != null && !suggestion.room().isBlank()
                    && suggestion.bed() != null && !suggestion.bed().isBlank()) {
                admission.setRoom(suggestion.room());
                admission.setBed(suggestion.bed());
            } else {
                admission.setRoom(null);
                admission.setBed(null);
            }
        } else {
            admission.setRoom(blankToNull(request.room()));
            admission.setBed(blankToNull(request.bed()));
        }
        admission.setReason(request.reason());
        admission.setAdmissionDateTime(LocalDateTime.now());
        admission.setStatus(AdmissionStatus.EN_COURS);
        Admission saved = admissionRepository.save(admission);
        saveMovement(saved.getId(), "ADMISSION", null, saved.getServiceName(), "Admission creee");
        return toResponse(saved);
    }

    /**
     * Proposition de chambre/lit pour le formulaire d'admission (occupation + capacité du service).
     */
    public BedSuggestionResponse suggestBed(String serviceName, String username) {
        userHospitalScopeService.assertCanUseServiceName(username, serviceName);
        return bedAllocationService.suggest(serviceName);
    }

    public AdmissionResponse getById(Long id, String username) {
        return toResponse(findEntityForUser(id, username));
    }

    public Page<AdmissionResponse> list(Long patientId, String status, String sortBy, String sortDir, Integer page, Integer size, String username) {
        Sort sort = buildSafeSort(sortBy, sortDir);
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 || size > 500 ? 15 : size;
        PageRequest pageable = PageRequest.of(safePage, safeSize, sort);
        Optional<Set<String>> scopeOpt = userHospitalScopeService.restrictedServiceNames(username);
        Page<Admission> entityPage;
        if (scopeOpt.isPresent()) {
            Collection<String> scope = scopeOpt.get();
            if (patientId != null && status != null && !status.isBlank()) {
                entityPage = admissionRepository.findByPatientIdAndStatusAndServiceNameIn(patientId, parseStatus(status), scope, pageable);
            } else if (patientId != null) {
                entityPage = admissionRepository.findByPatientIdAndServiceNameIn(patientId, scope, pageable);
            } else if (status != null && !status.isBlank()) {
                entityPage = admissionRepository.findByStatusAndServiceNameIn(parseStatus(status), scope, pageable);
            } else {
                entityPage = admissionRepository.findByServiceNameIn(scope, pageable);
            }
        } else if (patientId != null && status != null && !status.isBlank()) {
            entityPage = admissionRepository.findByPatientIdAndStatus(patientId, parseStatus(status), pageable);
        } else if (patientId != null) {
            entityPage = admissionRepository.findByPatientId(patientId, pageable);
        } else if (status != null && !status.isBlank()) {
            entityPage = admissionRepository.findByStatus(parseStatus(status), pageable);
        } else {
            entityPage = admissionRepository.findAll(pageable);
        }
        return entityPage.map(this::toResponse);
    }

    private Sort buildSafeSort(String sortBy, String sortDir) {
        Set<String> allowed = Set.of("id", "patientId", "serviceName", "admissionDateTime", "dischargeDateTime", "status");
        String field = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();
        if (!allowed.contains(field)) field = "id";

        boolean desc = sortDir == null || sortDir.isBlank() || sortDir.equalsIgnoreCase("desc");
        Sort.Direction dir = desc ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }

    /**
     * Moves patient to another service/room; status becomes {@code TRANSFERE}.
     * <p>Français : transfert vers un autre service / chambre ; statut {@code TRANSFERE}.</p>
     */
    public AdmissionResponse transfer(Long id, TransferRequest request, String username) {
        Admission admission = findEntityForUser(id, username);
        userHospitalScopeService.assertCanUseServiceName(username, request.toService());
        ensureAdmissionOpen(admission);
        patientLivingGuard.ensureAlive(admission.getPatientId());
        ensureActiveHospitalService(request.toService());
        String from = admission.getServiceName();
        admission.setServiceName(request.toService());
        boolean roomBlank = request.room() == null || request.room().isBlank();
        boolean bedBlank = request.bed() == null || request.bed().isBlank();
        if (roomBlank && bedBlank) {
            var suggestion = bedAllocationService.suggest(request.toService().trim());
            if (Boolean.TRUE.equals(suggestion.available())
                    && suggestion.room() != null && !suggestion.room().isBlank()
                    && suggestion.bed() != null && !suggestion.bed().isBlank()) {
                admission.setRoom(suggestion.room());
                admission.setBed(suggestion.bed());
            } else {
                admission.setRoom(null);
                admission.setBed(null);
            }
        } else {
            admission.setRoom(blankToNull(request.room()));
            admission.setBed(blankToNull(request.bed()));
        }
        admission.setStatus(AdmissionStatus.TRANSFERE);
        Admission saved = admissionRepository.save(admission);
        saveMovement(saved.getId(), "TRANSFERT", from, request.toService(), request.note());
        return toResponse(saved);
    }

    /**
     * Closes stay as {@code SORTI}; further clinical actions on this admission should be rejected.
     * <p>Français : clôture en {@code SORTI} ; les actes cliniques sur cette admission doivent être refusés ensuite.</p>
     */
    public AdmissionResponse discharge(Long id, DischargeRequest request, String username) {
        Admission admission = findEntityForUser(id, username);
        ensureAdmissionOpen(admission);
        admission.setStatus(AdmissionStatus.SORTI);
        admission.setDischargeDateTime(LocalDateTime.now());
        releaseBedAssignment(admission);
        Admission saved = admissionRepository.save(admission);
        saveMovement(saved.getId(), "SORTIE", saved.getServiceName(), null, request.note());
        return toResponse(saved);
    }

    /**
     * Closes stay as {@code DECEDE}; same immutability rules as discharge.
     * <p>Français : clôture en {@code DECEDE} ; même règles d'immuabilité qu'après sortie.</p>
     */
    public AdmissionResponse declareDeath(Long id, DeathDeclarationRequest request, String username) {
        Admission admission = findEntityForUser(id, username);
        Patient patient = patientRepository.findById(admission.getPatientId())
                .orElseThrow(() -> new NotFoundException("Patient introuvable: " + admission.getPatientId()));
        if (patient.getDeceasedAt() != null) {
            throw new ConflictException("Deces deja enregistre pour ce patient.");
        }
        ensureAdmissionOpen(admission);
        LocalDateTime closedAt = LocalDateTime.now();
        admission.setStatus(AdmissionStatus.DECEDE);
        admission.setDischargeDateTime(closedAt);
        releaseBedAssignment(admission);
        Admission saved = admissionRepository.save(admission);
        patient.setDeceasedAt(closedAt);
        patientRepository.save(patient);
        saveMovement(saved.getId(), "DECES", saved.getServiceName(), null, request.note());
        return toResponse(saved);
    }

    public LengthOfStayResponse lengthOfStay(Long id, String username) {
        Admission admission = findEntityForUser(id, username);
        long days = calculatorService.calculateCalendarDays(admission.getAdmissionDateTime(), admission.getDischargeDateTime());
        return new LengthOfStayResponse(admission.getId(), admission.getAdmissionDateTime().toLocalDate(), admission.getDischargeDateTime() == null ? null : admission.getDischargeDateTime().toLocalDate(), days);
    }

    public List<MovementResponse> movements(Long id, String username) {
        findEntityForUser(id, username);
        return movementRepository.findByAdmissionIdOrderByCreatedAtAsc(id).stream().map(m -> new MovementResponse(m.getId(), m.getAdmissionId(), m.getType(), m.getFromService(), m.getToService(), m.getCreatedAt(), m.getNote())).toList();
    }

    /**
     * Append-only trace for compliance and length-of-stay audits.
     * <p>Français : journal append-only pour conformité et analyse de durée de séjour.</p>
     */
    private void saveMovement(Long admissionId, String type, String fromService, String toService, String note) {
        Movement movement = new Movement();
        movement.setAdmissionId(admissionId);
        movement.setType(type);
        movement.setFromService(fromService);
        movement.setToService(toService);
        movement.setCreatedAt(LocalDateTime.now());
        movement.setNote(note);
        movementRepository.save(movement);
    }

    private Admission findEntity(Long id) {
        return admissionRepository.findById(id).orElseThrow(() -> new NotFoundException("Admission introuvable: " + id));
    }

    private Admission findEntityForUser(Long id, String username) {
        Admission admission = findEntity(id);
        userHospitalScopeService.assertCanAccessAdmission(username, admission);
        return admission;
    }

    private void ensurePatientExists(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new NotFoundException("Patient introuvable: " + patientId);
        }
    }

    private void ensureAdmissionOpen(Admission admission) {
        if (admission.getStatus() == AdmissionStatus.SORTI || admission.getStatus() == AdmissionStatus.DECEDE) throw new ConflictException("Admission deja fermee");
    }

    private AdmissionStatus parseStatus(String status) {
        try { return AdmissionStatus.valueOf(status.toUpperCase()); }
        catch (IllegalArgumentException ex) { throw new ConflictException("Statut invalide: " + status); }
    }

    private void ensureActiveHospitalService(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            throw new ConflictException("Service hospitalier invalide");
        }
        boolean existsActive = hospitalServiceRepository.findByNameIgnoreCase(serviceName.trim())
                .map(s -> s.isActive())
                .orElse(false);
        if (!existsActive) {
            throw new ConflictException("Service hospitalier introuvable ou inactif: " + serviceName);
        }
    }

    private AdmissionResponse toResponse(Admission admission) {
        return new AdmissionResponse(admission.getId(), admission.getPatientId(), admission.getServiceName(), admission.getRoom(), admission.getBed(), admission.getReason(), admission.getAdmissionDateTime(), admission.getDischargeDateTime(), admission.getStatus());
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    /** Libère chambre/lit en base lorsque le séjour se termine (sortie ou décès). */
    private static void releaseBedAssignment(Admission admission) {
        admission.setRoom(null);
        admission.setBed(null);
    }
}
