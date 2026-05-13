package com.afya.afya_health_system.soa.admission.repository;

import com.afya.afya_health_system.soa.admission.model.PrescriptionLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionLineRepository extends JpaRepository<PrescriptionLine, Long> {

    List<PrescriptionLine> findByAdmissionIdOrderByStartDateDescCreatedAtDesc(Long admissionId);
}
