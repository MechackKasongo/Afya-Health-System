package com.afya.afya_health_system.soa.urgence.repository;

import com.afya.afya_health_system.soa.urgence.model.Urgence;
import com.afya.afya_health_system.soa.urgence.model.UrgenceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UrgenceRepository extends JpaRepository<Urgence, Long> {
    List<Urgence> findByStatus(UrgenceStatus status, Sort sort);
    List<Urgence> findByPriority(String priority, Sort sort);
    List<Urgence> findByStatusAndPriority(UrgenceStatus status, String priority, Sort sort);

    Page<Urgence> findByStatus(UrgenceStatus status, Pageable pageable);
    Page<Urgence> findByPriority(String priority, Pageable pageable);
    Page<Urgence> findByStatusAndPriority(UrgenceStatus status, String priority, Pageable pageable);
}
