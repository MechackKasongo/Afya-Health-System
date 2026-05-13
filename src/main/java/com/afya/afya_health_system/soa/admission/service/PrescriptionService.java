package com.afya.afya_health_system.soa.admission.service;

import com.afya.afya_health_system.soa.admission.dto.MedicationAdministrationCreateRequest;
import com.afya.afya_health_system.soa.admission.dto.MedicationAdministrationResponse;
import com.afya.afya_health_system.soa.admission.dto.PrescriptionLineCreateRequest;
import com.afya.afya_health_system.soa.admission.dto.PrescriptionLineResponse;
import com.afya.afya_health_system.soa.admission.dto.PrescriptionLineUpdateRequest;
import com.afya.afya_health_system.soa.admission.model.MedicationAdministration;
import com.afya.afya_health_system.soa.admission.model.PrescriptionLine;
import com.afya.afya_health_system.soa.admission.repository.MedicationAdministrationRepository;
import com.afya.afya_health_system.soa.admission.repository.PrescriptionLineRepository;
import com.afya.afya_health_system.soa.common.exception.ConflictException;
import com.afya.afya_health_system.soa.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Inpatient prescription lines and per-dose administration marks for one admission.
 * <p>Français : lignes de prescription et coches d'administration (date + créneau) pour une admission.</p>
 */
@Service
@Transactional
public class PrescriptionService {

    private final PrescriptionLineRepository prescriptionLineRepository;
    private final MedicationAdministrationRepository medicationAdministrationRepository;
    private final AdmissionEligibilityService admissionEligibilityService;

    public PrescriptionService(
            PrescriptionLineRepository prescriptionLineRepository,
            MedicationAdministrationRepository medicationAdministrationRepository,
            AdmissionEligibilityService admissionEligibilityService
    ) {
        this.prescriptionLineRepository = prescriptionLineRepository;
        this.medicationAdministrationRepository = medicationAdministrationRepository;
        this.admissionEligibilityService = admissionEligibilityService;
    }

    public List<PrescriptionLineResponse> list(Long admissionId, String username) {
        admissionEligibilityService.requireAdmissionAccessible(admissionId, username);
        return prescriptionLineRepository.findByAdmissionIdOrderByStartDateDescCreatedAtDesc(admissionId).stream()
                .map(this::toLineResponse)
                .toList();
    }

    public PrescriptionLineResponse create(Long admissionId, PrescriptionLineCreateRequest request, String username) {
        admissionEligibilityService.requireOpenForClinicalWrite(admissionId, username);
        PrescriptionLine line = new PrescriptionLine();
        line.setAdmissionId(admissionId);
        applyCreate(line, request);
        line.setCreatedAt(LocalDateTime.now());
        line.setActive(true);
        return toLineResponse(prescriptionLineRepository.save(line));
    }

    public PrescriptionLineResponse update(Long admissionId, Long lineId, PrescriptionLineUpdateRequest request, String username) {
        admissionEligibilityService.requireOpenForClinicalWrite(admissionId, username);
        PrescriptionLine line = findLineForAdmission(admissionId, lineId);
        line.setMedicationName(request.medicationName());
        line.setDosageText(request.dosageText());
        line.setFrequencyText(request.frequencyText());
        line.setInstructionsText(request.instructionsText());
        line.setPrescriberName(request.prescriberName());
        line.setStartDate(request.startDate());
        line.setEndDate(request.endDate());
        line.setActive(request.active());
        return toLineResponse(prescriptionLineRepository.save(line));
    }

    public List<MedicationAdministrationResponse> listAdministrations(Long admissionId, Long lineId, String username) {
        admissionEligibilityService.requireAdmissionAccessible(admissionId, username);
        findLineForAdmission(admissionId, lineId);
        return medicationAdministrationRepository.findByPrescriptionLineIdOrderByAdministrationDateDescSlotAsc(lineId).stream()
                .map(this::toAdminResponse)
                .toList();
    }

    public MedicationAdministrationResponse recordAdministration(
            Long admissionId,
            Long lineId,
            MedicationAdministrationCreateRequest request,
            String username
    ) {
        admissionEligibilityService.requireOpenForClinicalWrite(admissionId, username);
        findLineForAdmission(admissionId, lineId);
        var existing = medicationAdministrationRepository.findByPrescriptionLineIdAndAdministrationDateAndSlot(
                lineId, request.administrationDate(), request.slot());
        MedicationAdministration row;
        if (existing.isPresent()) {
            row = existing.get();
            row.setAdministered(request.administered());
        } else {
            row = new MedicationAdministration();
            row.setPrescriptionLineId(lineId);
            row.setAdministrationDate(request.administrationDate());
            row.setSlot(request.slot());
            row.setAdministered(request.administered());
        }
        return toAdminResponse(medicationAdministrationRepository.save(row));
    }

    private void applyCreate(PrescriptionLine line, PrescriptionLineCreateRequest request) {
        line.setMedicationName(request.medicationName());
        line.setDosageText(request.dosageText());
        line.setFrequencyText(request.frequencyText());
        line.setInstructionsText(request.instructionsText());
        line.setPrescriberName(request.prescriberName());
        line.setStartDate(request.startDate());
        line.setEndDate(request.endDate());
    }

    private PrescriptionLine findLineForAdmission(Long admissionId, Long lineId) {
        PrescriptionLine line = prescriptionLineRepository.findById(lineId)
                .orElseThrow(() -> new NotFoundException("Ligne de prescription introuvable: " + lineId));
        if (!line.getAdmissionId().equals(admissionId)) {
            throw new ConflictException("La ligne ne correspond pas a cette admission");
        }
        return line;
    }

    private PrescriptionLineResponse toLineResponse(PrescriptionLine line) {
        return new PrescriptionLineResponse(
                line.getId(),
                line.getAdmissionId(),
                line.getMedicationName(),
                line.getDosageText(),
                line.getFrequencyText(),
                line.getInstructionsText(),
                line.getPrescriberName(),
                line.getStartDate(),
                line.getEndDate(),
                line.isActive(),
                line.getCreatedAt()
        );
    }

    private MedicationAdministrationResponse toAdminResponse(MedicationAdministration m) {
        return new MedicationAdministrationResponse(
                m.getId(),
                m.getPrescriptionLineId(),
                m.getAdministrationDate(),
                m.getSlot(),
                m.isAdministered()
        );
    }
}
