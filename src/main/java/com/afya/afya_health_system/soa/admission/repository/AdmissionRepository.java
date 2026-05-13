package com.afya.afya_health_system.soa.admission.repository;

import com.afya.afya_health_system.soa.admission.model.Admission;
import com.afya.afya_health_system.soa.admission.model.AdmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;

public interface AdmissionRepository extends JpaRepository<Admission, Long> {
    interface ServiceOccupancyRow {
        String getServiceName();
        long getOccupiedBeds();
    }

    List<Admission> findByPatientId(Long patientId, Sort sort);
    List<Admission> findByStatus(AdmissionStatus status, Sort sort);
    List<Admission> findByPatientIdAndStatus(Long patientId, AdmissionStatus status, Sort sort);

    Page<Admission> findByPatientId(Long patientId, Pageable pageable);
    Page<Admission> findByStatus(AdmissionStatus status, Pageable pageable);
    Page<Admission> findByPatientIdAndStatus(Long patientId, AdmissionStatus status, Pageable pageable);

    Page<Admission> findByServiceNameIn(Collection<String> serviceNames, Pageable pageable);

    Page<Admission> findByPatientIdAndServiceNameIn(Long patientId, Collection<String> serviceNames, Pageable pageable);

    Page<Admission> findByStatusAndServiceNameIn(AdmissionStatus status, Collection<String> serviceNames, Pageable pageable);

    Page<Admission> findByPatientIdAndStatusAndServiceNameIn(
            Long patientId,
            AdmissionStatus status,
            Collection<String> serviceNames,
            Pageable pageable
    );

    boolean existsByServiceNameIgnoreCase(String serviceName);

    @Query("select a.serviceName as serviceName, count(a) as occupiedBeds " +
            "from Admission a where a.status in :statuses group by a.serviceName")
    List<ServiceOccupancyRow> countOccupiedBedsByService(@Param("statuses") Collection<AdmissionStatus> statuses);

    long countByServiceNameIgnoreCaseAndStatusIn(String serviceName, Collection<AdmissionStatus> statuses);
}
