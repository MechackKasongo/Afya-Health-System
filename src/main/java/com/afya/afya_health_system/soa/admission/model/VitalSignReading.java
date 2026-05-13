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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Structured vital signs and observation row linked to one inpatient admission (French chart equivalents:
 * T.A., pouls, température, poids, diurèse, selles).
 * <p>Français : ligne de constantes / surveillance liée à une admission (TA, pouls, T°, poids, diurèse, selles).</p>
 */
@Entity
@Table(name = "vital_sign_readings", indexes = {
        @Index(name = "idx_vsr_admission_recorded", columnList = "admission_id,recorded_at")
})
public class VitalSignReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admission_id", nullable = false)
    private Long admissionId;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot", length = 20)
    private VitalSignSlot slot;

    @Column(name = "systolic_bp")
    private Integer systolicBp;

    @Column(name = "diastolic_bp")
    private Integer diastolicBp;

    @Column(name = "pulse_bpm")
    private Integer pulseBpm;

    @Column(name = "temperature_celsius", precision = 5, scale = 2)
    private BigDecimal temperatureCelsius;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "diuresis_ml")
    private Integer diuresisMl;

    @Column(name = "stools_note", length = 500)
    private String stoolsNote;

    public Long getId() {
        return id;
    }

    public Long getAdmissionId() {
        return admissionId;
    }

    public void setAdmissionId(Long admissionId) {
        this.admissionId = admissionId;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public VitalSignSlot getSlot() {
        return slot;
    }

    public void setSlot(VitalSignSlot slot) {
        this.slot = slot;
    }

    public Integer getSystolicBp() {
        return systolicBp;
    }

    public void setSystolicBp(Integer systolicBp) {
        this.systolicBp = systolicBp;
    }

    public Integer getDiastolicBp() {
        return diastolicBp;
    }

    public void setDiastolicBp(Integer diastolicBp) {
        this.diastolicBp = diastolicBp;
    }

    public Integer getPulseBpm() {
        return pulseBpm;
    }

    public void setPulseBpm(Integer pulseBpm) {
        this.pulseBpm = pulseBpm;
    }

    public BigDecimal getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public void setTemperatureCelsius(BigDecimal temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public Integer getDiuresisMl() {
        return diuresisMl;
    }

    public void setDiuresisMl(Integer diuresisMl) {
        this.diuresisMl = diuresisMl;
    }

    public String getStoolsNote() {
        return stoolsNote;
    }

    public void setStoolsNote(String stoolsNote) {
        this.stoolsNote = stoolsNote;
    }
}
