package com.afya.afya_health_system.soa.admission.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admission_movements")
public class Movement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long admissionId;
    @Column(nullable = false, length = 20)
    private String type;
    @Column(length = 80)
    private String fromService;
    @Column(length = 80)
    private String toService;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(length = 255)
    private String note;

    public Long getId() { return id; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getFromService() { return fromService; }
    public void setFromService(String fromService) { this.fromService = fromService; }
    public String getToService() { return toService; }
    public void setToService(String toService) { this.toService = toService; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
