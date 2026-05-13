package com.afya.afya_health_system.soa.patient.repository;

import com.afya.afya_health_system.soa.patient.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    boolean existsByDossierNumber(String dossierNumber);
    Optional<Patient> findTopByDossierNumberStartingWithOrderByDossierNumberDesc(String prefix);

    @Query("""
            SELECT p FROM Patient p WHERE
            LOWER(p.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(p.dossierNumber) LIKE LOWER(CONCAT('%', :q, '%'))
            OR (p.postName IS NOT NULL AND LOWER(p.postName) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Patient> searchByText(@Param("q") String q, Pageable pageable);

    /**
     * Check for duplicate patients by comparing key identifying fields:
     * firstName, lastName, birthDate, sex, postName, and phone.
     * Used during patient registration to prevent duplicates.
     * <p>Français : vérifie les doublons en comparant les champs identifiants clés.</p>
     */
    @Query("""
            SELECT p FROM Patient p WHERE
            LOWER(p.firstName) = LOWER(:firstName)
            AND LOWER(p.lastName) = LOWER(:lastName)
            AND p.birthDate = :birthDate
            AND LOWER(p.sex) = LOWER(:sex)
            AND LOWER(COALESCE(p.postName, '')) = LOWER(COALESCE(:postName, ''))
            AND LOWER(COALESCE(p.phone, '')) = LOWER(COALESCE(:phone, ''))
            """)
    List<Patient> findDuplicatePatients(
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("birthDate") LocalDate birthDate,
            @Param("sex") String sex,
            @Param("postName") String postName,
            @Param("phone") String phone
    );
}
