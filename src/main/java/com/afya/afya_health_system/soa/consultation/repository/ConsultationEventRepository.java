package com.afya.afya_health_system.soa.consultation.repository;

import com.afya.afya_health_system.soa.consultation.model.ConsultationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultationEventRepository extends JpaRepository<ConsultationEvent, Long> {
    List<ConsultationEvent> findByPatientIdOrderByCreatedAtAsc(Long patientId);
}
