package com.afya.afya_health_system.soa.hospitalservice.repository;

import com.afya.afya_health_system.soa.hospitalservice.model.HospitalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HospitalServiceRepository extends JpaRepository<HospitalService, Long> {
    Optional<HospitalService> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<HospitalService> findByActiveTrueOrderByNameAsc();
    List<HospitalService> findAllByOrderByNameAsc();

    Page<HospitalService> findByActiveTrueOrderByNameAsc(Pageable pageable);
    Page<HospitalService> findAllByOrderByNameAsc(Pageable pageable);
}
