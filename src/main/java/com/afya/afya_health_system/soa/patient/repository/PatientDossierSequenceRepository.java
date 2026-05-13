package com.afya.afya_health_system.soa.patient.repository;

import com.afya.afya_health_system.soa.patient.model.PatientDossierSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PatientDossierSequenceRepository extends JpaRepository<PatientDossierSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PatientDossierSequence s where s.sequenceYear = :year")
    Optional<PatientDossierSequence> findByYearForUpdate(Integer year);
}
