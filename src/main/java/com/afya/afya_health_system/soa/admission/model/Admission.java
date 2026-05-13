package com.afya.afya_health_system.soa.admission.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admissions")
public class Admission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long patientId;
    @Column(nullable = false, length = 80)
    private String serviceName;
    @Column(length = 20)
    private String room;
    @Column(length = 20)
    private String bed;
    @Column(length = 255)
    private String reason;
    @Column(nullable = false)
    private LocalDateTime admissionDateTime;
    private LocalDateTime dischargeDateTime;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdmissionStatus status;

    public Long getId() { return id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public String getBed() { return bed; }
    public void setBed(String bed) { this.bed = bed; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getAdmissionDateTime() { return admissionDateTime; }
    public void setAdmissionDateTime(LocalDateTime admissionDateTime) { this.admissionDateTime = admissionDateTime; }
    public LocalDateTime getDischargeDateTime() { return dischargeDateTime; }
    public void setDischargeDateTime(LocalDateTime dischargeDateTime) { this.dischargeDateTime = dischargeDateTime; }
    public AdmissionStatus getStatus() { return status; }
    public void setStatus(AdmissionStatus status) { this.status = status; }
}
