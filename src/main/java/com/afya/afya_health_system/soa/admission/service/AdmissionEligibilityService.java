package com.afya.afya_health_system.soa.admission.service;

import com.afya.afya_health_system.soa.admission.model.Admission;
import com.afya.afya_health_system.soa.admission.model.AdmissionStatus;
import com.afya.afya_health_system.soa.admission.repository.AdmissionRepository;
import com.afya.afya_health_system.soa.common.exception.ConflictException;
import com.afya.afya_health_system.soa.common.exception.NotFoundException;
import com.afya.afya_health_system.soa.patient.service.PatientLivingGuard;
import org.springframework.stereotype.Service;

/**
 * Centralizes rules for whether clinical data (vitals, prescriptions, clinical form) may still be edited.
 * <p>Français : règles pour savoir si une admission accepte encore des saisies cliniques (pas après sortie ou décès).</p>
 */
@Service
public class AdmissionEligibilityService {

    private final AdmissionRepository admissionRepository;
    private final PatientLivingGuard patientLivingGuard;
    private final UserHospitalScopeService userHospitalScopeService;

    public AdmissionEligibilityService(
            AdmissionRepository admissionRepository,
            PatientLivingGuard patientLivingGuard,
            UserHospitalScopeService userHospitalScopeService
    ) {
        this.admissionRepository = admissionRepository;
        this.patientLivingGuard = patientLivingGuard;
        this.userHospitalScopeService = userHospitalScopeService;
    }

    public Admission requireAdmissionAccessible(Long admissionId, String username) {
        Admission a = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new NotFoundException("Admission introuvable: " + admissionId));
        userHospitalScopeService.assertCanAccessAdmission(username, a);
        return a;
    }

    /**
     * Blocks creates/updates when the stay is already closed (discharge or death).
     * <p>Français : interdit toute nouvelle saisie clinique si le séjour est clôturé (sortie ou décès).</p>
     */
    public void requireOpenForClinicalWrite(Long admissionId, String username) {
        Admission a = requireAdmissionAccessible(admissionId, username);
        if (a.getStatus() == AdmissionStatus.SORTI || a.getStatus() == AdmissionStatus.DECEDE) {
            throw new ConflictException("Admission terminée : la saisie clinique n'est plus autorisée.");
        }
        patientLivingGuard.ensureAlive(a.getPatientId());
    }
}
