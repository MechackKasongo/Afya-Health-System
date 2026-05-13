package com.afya.afya_health_system.soa.urgence.repository;

import com.afya.afya_health_system.soa.urgence.model.UrgenceTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UrgenceTimelineEventRepository extends JpaRepository<UrgenceTimelineEvent, Long> {
    List<UrgenceTimelineEvent> findByUrgenceIdOrderByCreatedAtAsc(Long urgenceId);

    List<UrgenceTimelineEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
