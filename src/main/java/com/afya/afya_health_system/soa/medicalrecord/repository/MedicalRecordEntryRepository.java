package com.afya.afya_health_system.soa.medicalrecord.repository;

import com.afya.afya_health_system.soa.medicalrecord.model.MedicalRecordEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalRecordEntryRepository extends JpaRepository<MedicalRecordEntry, Long> {
    List<MedicalRecordEntry> findByPatientIdOrderByCreatedAtAsc(Long patientId);
}
