package com.afya.afya_health_system.soa.admission.repository;

import com.afya.afya_health_system.soa.admission.model.AdmissionClinicalForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdmissionClinicalFormRepository extends JpaRepository<AdmissionClinicalForm, Long> {

    Optional<AdmissionClinicalForm> findByAdmissionId(Long admissionId);
}
