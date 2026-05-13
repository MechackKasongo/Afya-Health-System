package com.afya.afya_health_system.soa.admission.service;

import com.afya.afya_health_system.soa.admission.dto.AdmissionClinicalFormResponse;
import com.afya.afya_health_system.soa.admission.dto.AdmissionClinicalFormUpsertRequest;
import com.afya.afya_health_system.soa.admission.model.AdmissionClinicalForm;
import com.afya.afya_health_system.soa.admission.repository.AdmissionClinicalFormRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Structured hospitalization narrative (one block per section) stored per admission.
 * <p>Français : formulaire d'hospitalisation par sections texte (antécédents, examens, etc.).</p>
 */
@Service
@Transactional
public class AdmissionClinicalFormService {

    private final AdmissionClinicalFormRepository admissionClinicalFormRepository;
    private final AdmissionEligibilityService admissionEligibilityService;

    public AdmissionClinicalFormService(
            AdmissionClinicalFormRepository admissionClinicalFormRepository,
            AdmissionEligibilityService admissionEligibilityService
    ) {
        this.admissionClinicalFormRepository = admissionClinicalFormRepository;
        this.admissionEligibilityService = admissionEligibilityService;
    }

    public AdmissionClinicalFormResponse get(Long admissionId, String username) {
        admissionEligibilityService.requireAdmissionAccessible(admissionId, username);
        return admissionClinicalFormRepository.findByAdmissionId(admissionId)
                .map(this::toResponse)
                .orElseGet(() -> emptyShell(admissionId));
    }

    public AdmissionClinicalFormResponse upsert(Long admissionId, AdmissionClinicalFormUpsertRequest request, String username) {
        admissionEligibilityService.requireOpenForClinicalWrite(admissionId, username);
        AdmissionClinicalForm form = admissionClinicalFormRepository.findByAdmissionId(admissionId)
                .orElseGet(() -> {
                    AdmissionClinicalForm f = new AdmissionClinicalForm();
                    f.setAdmissionId(admissionId);
                    return f;
                });
        form.setAntecedentsText(request.antecedentsText());
        form.setAnamnesisText(request.anamnesisText());
        form.setPhysicalExamPulmonaryText(request.physicalExamPulmonaryText());
        form.setPhysicalExamCardiacText(request.physicalExamCardiacText());
        form.setPhysicalExamAbdominalText(request.physicalExamAbdominalText());
        form.setPhysicalExamNeurologicalText(request.physicalExamNeurologicalText());
        form.setPhysicalExamMiscText(request.physicalExamMiscText());
        form.setParaclinicalText(request.paraclinicalText());
        form.setConclusionText(request.conclusionText());
        return toResponse(admissionClinicalFormRepository.save(form));
    }

    private AdmissionClinicalFormResponse emptyShell(Long admissionId) {
        return new AdmissionClinicalFormResponse(
                null,
                admissionId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private AdmissionClinicalFormResponse toResponse(AdmissionClinicalForm f) {
        return new AdmissionClinicalFormResponse(
                f.getId(),
                f.getAdmissionId(),
                f.getAntecedentsText(),
                f.getAnamnesisText(),
                f.getPhysicalExamPulmonaryText(),
                f.getPhysicalExamCardiacText(),
                f.getPhysicalExamAbdominalText(),
                f.getPhysicalExamNeurologicalText(),
                f.getPhysicalExamMiscText(),
                f.getParaclinicalText(),
                f.getConclusionText()
        );
    }
}
