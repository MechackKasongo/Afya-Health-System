package com.afya.afya_health_system.soa.consultation.repository;

import com.afya.afya_health_system.soa.consultation.model.Consultation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    List<Consultation> findByPatientId(Long patientId, Sort sort);
    List<Consultation> findByAdmissionId(Long admissionId, Sort sort);
    List<Consultation> findByPatientIdAndAdmissionId(Long patientId, Long admissionId, Sort sort);

    Page<Consultation> findByPatientId(Long patientId, Pageable pageable);
    Page<Consultation> findByAdmissionId(Long admissionId, Pageable pageable);
    Page<Consultation> findByPatientIdAndAdmissionId(Long patientId, Long admissionId, Pageable pageable);
}
