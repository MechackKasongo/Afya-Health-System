package com.afya.afya_health_system.soa.admission.repository;

import com.afya.afya_health_system.soa.admission.model.Movement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MovementRepository extends JpaRepository<Movement, Long> {
    List<Movement> findByAdmissionIdOrderByCreatedAtAsc(Long admissionId);

    List<Movement> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
