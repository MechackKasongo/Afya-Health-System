package com.afya.afya_health_system.soa.urgence.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "urgence_timeline_events")
public class UrgenceTimelineEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long urgenceId;
    @Column(nullable = false, length = 40)
    private String type;
    @Column(length = 255)
    private String details;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Long getUrgenceId() { return urgenceId; }
    public void setUrgenceId(Long urgenceId) { this.urgenceId = urgenceId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
