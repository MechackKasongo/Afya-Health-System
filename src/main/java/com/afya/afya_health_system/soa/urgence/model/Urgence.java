package com.afya.afya_health_system.soa.urgence.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "urgences")
public class Urgence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long patientId;
    @Column(length = 255)
    private String motif;
    @Column(length = 40)
    private String priority;
    @Column(length = 40)
    private String triageLevel;
    @Column(length = 80)
    private String orientation;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UrgenceStatus status;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;

    public Long getId() { return id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getTriageLevel() { return triageLevel; }
    public void setTriageLevel(String triageLevel) { this.triageLevel = triageLevel; }
    public String getOrientation() { return orientation; }
    public void setOrientation(String orientation) { this.orientation = orientation; }
    public UrgenceStatus getStatus() { return status; }
    public void setStatus(UrgenceStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
}
