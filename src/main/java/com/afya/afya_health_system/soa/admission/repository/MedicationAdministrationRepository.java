package com.afya.afya_health_system.soa.admission.repository;

import com.afya.afya_health_system.soa.admission.model.MedicationAdministration;
import com.afya.afya_health_system.soa.admission.model.VitalSignSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MedicationAdministrationRepository extends JpaRepository<MedicationAdministration, Long> {

    List<MedicationAdministration> findByPrescriptionLineIdOrderByAdministrationDateDescSlotAsc(Long prescriptionLineId);

    Optional<MedicationAdministration> findByPrescriptionLineIdAndAdministrationDateAndSlot(
            Long prescriptionLineId,
            LocalDate administrationDate,
            VitalSignSlot slot
    );
}
