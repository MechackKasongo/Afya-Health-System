package com.afya.afya_health_system.soa.admission.repository;

import com.afya.afya_health_system.soa.admission.model.VitalSignReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VitalSignReadingRepository extends JpaRepository<VitalSignReading, Long> {

    List<VitalSignReading> findByAdmissionIdOrderByRecordedAtDesc(Long admissionId);
}
