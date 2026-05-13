package com.afya.afya_health_system.soa.patient.service;

import com.afya.afya_health_system.soa.common.exception.ConflictException;
import com.afya.afya_health_system.soa.common.exception.NotFoundException;
import com.afya.afya_health_system.soa.patient.model.Patient;
import com.afya.afya_health_system.soa.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;

/**
 * Refuse les écritures métier lorsque {@link Patient#getDeceasedAt()} est renseigné.
 * <p>Français : garde-fou unique pour éviter les modifications après enregistrement du décès.</p>
 */
@Service
public class PatientLivingGuard {

    private final PatientRepository patientRepository;

    public PatientLivingGuard(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public void ensureAlive(Long patientId) {
        if (patientId == null) {
            return;
        }
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient introuvable: " + patientId));
        ensureAlive(patient);
    }

    public void ensureAlive(Patient patient) {
        if (patient.getDeceasedAt() != null) {
            throw new ConflictException("Patient decede : modification ou nouvelle saisie non autorisee.");
        }
    }
}
