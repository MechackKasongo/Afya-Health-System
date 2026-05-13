package com.afya.afya_health_system.soa.reporting.dto;

/**
 * DTO d'événement d'audit agrégeant plusieurs sources (admissions, urgences).
 *
 * Fr: l'API renvoie une liste d'événements "append-only" pour visualiser le fil d'activité.
 */
public record AuditEventResponse(
        Long id,
        String source,
        String entityType,
        Long entityId,
        String eventType,
        String message,
        String createdAt
) {}

