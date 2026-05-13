package com.afya.afya_health_system.soa.consultation.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "consultation_events")
public class ConsultationEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long consultationId;
    @Column(nullable = false) private Long patientId;
    @Column(nullable = false, length = 40) private String type;
    @Lob
    @Column(nullable = false, columnDefinition = "CLOB") private String content;
    @Column(nullable = false) private LocalDateTime createdAt;
    public Long getId() { return id; }
    public Long getConsultationId() { return consultationId; }
    public void setConsultationId(Long consultationId) { this.consultationId = consultationId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
