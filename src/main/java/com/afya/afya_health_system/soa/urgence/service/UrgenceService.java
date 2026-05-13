package com.afya.afya_health_system.soa.urgence.service;

import com.afya.afya_health_system.soa.common.exception.ConflictException;
import com.afya.afya_health_system.soa.common.exception.NotFoundException;
import com.afya.afya_health_system.soa.urgence.dto.*;
import com.afya.afya_health_system.soa.urgence.model.Urgence;
import com.afya.afya_health_system.soa.urgence.model.UrgenceStatus;
import com.afya.afya_health_system.soa.urgence.model.UrgenceTimelineEvent;
import com.afya.afya_health_system.soa.patient.repository.PatientRepository;
import com.afya.afya_health_system.soa.admission.service.UserHospitalScopeService;
import com.afya.afya_health_system.soa.urgence.repository.UrgenceRepository;
import com.afya.afya_health_system.soa.urgence.repository.UrgenceTimelineEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Emergency visit lifecycle: registration, triage, orientation, closure. Status transitions are
 * guarded so a closed visit cannot be mutated; each step appends a timeline row for traceability.
 * <p>Français : cycle des passages aux urgences (enregistrement, triage, orientation, clôture). Une visite
 * clôturée ne peut plus être modifiée ; chaque étape ajoute une entrée au fil chronologique.</p>
 */
@Service
@Transactional
public class UrgenceService {

    private static final Logger log = LoggerFactory.getLogger(UrgenceService.class);

    private final UrgenceRepository urgenceRepository;
    private final UrgenceTimelineEventRepository timelineRepository;
    private final PatientRepository patientRepository;
    private final UserHospitalScopeService userHospitalScopeService;

    public UrgenceService(
            UrgenceRepository urgenceRepository,
            UrgenceTimelineEventRepository timelineRepository,
            PatientRepository patientRepository,
            UserHospitalScopeService userHospitalScopeService
    ) {
        this.urgenceRepository = urgenceRepository;
        this.timelineRepository = timelineRepository;
        this.patientRepository = patientRepository;
        this.userHospitalScopeService = userHospitalScopeService;
    }

    /**
     * New visit in {@code EN_ATTENTE_TRIAGE} with an initial CREATED timeline event.
     * <p>Français : nouveau passage {@code EN_ATTENTE_TRIAGE} avec événement CREATED sur la ligne de temps.</p>
     */
    public UrgenceResponse create(UrgenceCreateRequest request, String username) {
        userHospitalScopeService.assertCanUseUrgences(username);
        ensurePatientExists(request.patientId());
        Urgence urgence = new Urgence();
        urgence.setPatientId(request.patientId());
        urgence.setMotif(request.motif());
        urgence.setPriority(request.priority());
        urgence.setStatus(UrgenceStatus.EN_ATTENTE_TRIAGE);
        urgence.setCreatedAt(LocalDateTime.now());
        Urgence saved = urgenceRepository.save(urgence);
        addTimeline(saved.getId(), "CREATED", "Urgence enregistree");
        return toResponse(saved);
    }
    public UrgenceResponse getById(Long id, String username) {
        return toResponse(findEntityForUser(id, username, "GET_DETAIL"));
    }
    /**
     * Optional filter by ER status or clinical priority; invalid status strings yield 409 via {@link ConflictException}.
     * {@link UrgenceBoardResponse#scopeRestricted()} is {@code true} when the list is empty because the user's assignment excludes « Urgences ».
     * <p>Français : filtre facultatif par statut ou priorité ; statut invalide → 409 {@link ConflictException}. Indicateur {@code scopeRestricted} si liste vide par affectation.</p>
     */
    public UrgenceBoardResponse list(String status, String priority, String sortBy, String sortDir, Integer page, Integer size, String username) {
        Sort sort = buildSafeSort(sortBy, sortDir);
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 || size > 500 ? 15 : size;
        PageRequest pageable = PageRequest.of(safePage, safeSize, sort);
        boolean hiddenByAssignment = userHospitalScopeService.isUrgencesListHiddenByAssignment(username);
        if (!userHospitalScopeService.hasUrgencesAccess(username)) {
            return UrgenceBoardResponse.from(Page.empty(pageable), hiddenByAssignment);
        }
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasPriority = priority != null && !priority.isBlank();
        Page<Urgence> entityPage;
        if (hasStatus && hasPriority) {
            entityPage = urgenceRepository.findByStatusAndPriority(parseStatus(status), priority, pageable);
        } else if (hasStatus) {
            entityPage = urgenceRepository.findByStatus(parseStatus(status), pageable);
        } else if (hasPriority) {
            entityPage = urgenceRepository.findByPriority(priority, pageable);
        } else {
            entityPage = urgenceRepository.findAll(pageable);
        }
        return UrgenceBoardResponse.from(entityPage.map(this::toResponse), false);
    }

    private Sort buildSafeSort(String sortBy, String sortDir) {
        Set<String> allowed = Set.of("id", "patientId", "priority", "status", "createdAt", "closedAt", "triageLevel", "orientation");
        String field = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();
        if (!allowed.contains(field)) field = "id";

        boolean desc = sortDir == null || sortDir.isBlank() || sortDir.equalsIgnoreCase("desc");
        Sort.Direction dir = desc ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }
    /**
     * Sets triage level and moves visit to {@code EN_COURS}.
     * <p>Français : niveau de triage et passage en {@code EN_COURS}.</p>
     */
    public UrgenceResponse triage(Long id, TriageRequest request, String username) {
        Urgence urgence = findEntityForUser(id, username, "TRIAGE");
        ensureNotClosed(urgence);
        urgence.setTriageLevel(request.triageLevel()); urgence.setStatus(UrgenceStatus.EN_COURS);
        Urgence saved = urgenceRepository.save(urgence); addTimeline(saved.getId(), "TRIAGE", request.details()); return toResponse(saved);
    }
    /**
     * Documents where the patient was sent (consultation, ICU, etc.) and sets {@code ORIENTE}.
     * <p>Français : consigne la destination du patient ; statut {@code ORIENTE}.</p>
     */
    public UrgenceResponse orient(Long id, OrientRequest request, String username) {
        Urgence urgence = findEntityForUser(id, username, "ORIENT");
        ensureNotClosed(urgence);
        urgence.setOrientation(request.orientation()); urgence.setStatus(UrgenceStatus.ORIENTE);
        Urgence saved = urgenceRepository.save(urgence); addTimeline(saved.getId(), "ORIENT", request.details()); return toResponse(saved);
    }
    /**
     * Terminal state {@code CLOTURE}; timestamp stored for reporting.
     * <p>Français : état terminal {@code CLOTURE} ; horodatage pour les indicateurs.</p>
     */
    public UrgenceResponse close(Long id, CloseRequest request, String username) {
        Urgence urgence = findEntityForUser(id, username, "CLOSE");
        ensureNotClosed(urgence);
        urgence.setStatus(UrgenceStatus.CLOTURE); urgence.setClosedAt(LocalDateTime.now());
        Urgence saved = urgenceRepository.save(urgence); addTimeline(saved.getId(), "CLOSED", request == null ? null : request.details()); return toResponse(saved);
    }
    public List<UrgenceTimelineEventResponse> timeline(Long id, String username) {
        findEntityForUser(id, username, "TIMELINE");
        return timelineRepository.findByUrgenceIdOrderByCreatedAtAsc(id).stream().map(e -> new UrgenceTimelineEventResponse(e.getId(), e.getUrgenceId(), e.getType(), e.getDetails(), e.getCreatedAt())).toList();
    }
    /**
     * Append-only audit strip; never delete rows from the service layer.
     * <p>Français : bandeau d'audit en append-only ; pas de suppression côté service.</p>
     */
    private void addTimeline(Long urgenceId, String type, String details) {
        UrgenceTimelineEvent event = new UrgenceTimelineEvent();
        event.setUrgenceId(urgenceId); event.setType(type); event.setDetails(details); event.setCreatedAt(LocalDateTime.now());
        timelineRepository.save(event);
    }
    private Urgence findEntity(Long id) { return urgenceRepository.findById(id).orElseThrow(() -> new NotFoundException("Urgence introuvable: " + id)); }

    private Urgence findEntityForUser(Long id, String username, String operation) {
        if (!userHospitalScopeService.hasUrgencesAccess(username)) {
            log.warn("Urgences access denied ({}): user={}, urgenceId={}", operation, username, id);
            throw new NotFoundException("Urgence introuvable: " + id);
        }
        return findEntity(id);
    }
    private void ensurePatientExists(Long patientId) {
        if (!patientRepository.existsById(patientId)) throw new NotFoundException("Patient introuvable: " + patientId);
    }
    private void ensureNotClosed(Urgence urgence) { if (urgence.getStatus() == UrgenceStatus.CLOTURE) throw new ConflictException("Urgence deja cloturee"); }
    private UrgenceStatus parseStatus(String status) {
        try { return UrgenceStatus.valueOf(status.toUpperCase()); }
        catch (IllegalArgumentException ex) { throw new ConflictException("Statut urgence invalide: " + status); }
    }
    private UrgenceResponse toResponse(Urgence urgence) {
        return new UrgenceResponse(urgence.getId(), urgence.getPatientId(), urgence.getMotif(), urgence.getPriority(), urgence.getTriageLevel(), urgence.getOrientation(), urgence.getStatus(), urgence.getCreatedAt(), urgence.getClosedAt());
    }
}
