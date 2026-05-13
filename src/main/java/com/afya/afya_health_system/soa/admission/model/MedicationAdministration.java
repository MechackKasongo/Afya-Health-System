package com.afya.afya_health_system.soa.admission.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

/**
 * Marks whether a scheduled dose was given (morning/evening slot) — matches tick marks on paper treatment charts.
 * <p>Français : administration réalisée ou non pour une date et un créneau (relevé type coches sur fiche de traitement).</p>
 */
@Entity
@Table(
        name = "medication_administrations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_med_admin_line_date_slot",
                columnNames = {"prescription_line_id", "administration_date", "slot"}
        ),
        indexes = {
                @Index(name = "idx_med_admin_line", columnList = "prescription_line_id")
        }
)
public class MedicationAdministration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prescription_line_id", nullable = false)
    private Long prescriptionLineId;

    @Column(name = "administration_date", nullable = false)
    private LocalDate administrationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VitalSignSlot slot;

    @Column(nullable = false)
    private boolean administered;

    public Long getId() {
        return id;
    }

    public Long getPrescriptionLineId() {
        return prescriptionLineId;
    }

    public void setPrescriptionLineId(Long prescriptionLineId) {
        this.prescriptionLineId = prescriptionLineId;
    }

    public LocalDate getAdministrationDate() {
        return administrationDate;
    }

    public void setAdministrationDate(LocalDate administrationDate) {
        this.administrationDate = administrationDate;
    }

    public VitalSignSlot getSlot() {
        return slot;
    }

    public void setSlot(VitalSignSlot slot) {
        this.slot = slot;
    }

    public boolean isAdministered() {
        return administered;
    }

    public void setAdministered(boolean administered) {
        this.administered = administered;
    }
}
