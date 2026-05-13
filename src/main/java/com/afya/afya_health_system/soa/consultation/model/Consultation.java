package com.afya.afya_health_system.soa.consultation.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "consultations")
public class Consultation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long patientId;
    @Column(nullable = false) private Long admissionId;
    @Column(nullable = false, length = 80) private String doctorName;
    @Column(length = 255) private String reason;
    @Column(nullable = false) private LocalDateTime consultationDateTime;
    public Long getId() { return id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getConsultationDateTime() { return consultationDateTime; }
    public void setConsultationDateTime(LocalDateTime consultationDateTime) { this.consultationDateTime = consultationDateTime; }
}
