package com.afya.afya_health_system.soa.urgence.controller;

import com.afya.afya_health_system.soa.urgence.dto.*;
import com.afya.afya_health_system.soa.urgence.service.UrgenceService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Emergency department workflow surface (create, triage, orient, close, timeline).
 * <p>Français : API urgences (création, triage, orientation, clôture, ligne de temps).</p>
 */
@RestController
@RequestMapping("/api/v1/urgences")
public class UrgenceController {

    private final UrgenceService urgenceService;

    public UrgenceController(UrgenceService urgenceService) {
        this.urgenceService = urgenceService;
    }

    /**
     * New ER presentation.
     * <p>Français : nouvelle prise en charge aux urgences.</p>
     */
    @PostMapping
    public UrgenceResponse create(@Valid @RequestBody UrgenceCreateRequest request, Authentication authentication) {
        return urgenceService.create(request, authentication.getName());
    }

    /**
     * Single visit.
     * <p>Français : détail d'une urgence.</p>
     */
    @GetMapping("/{id}")
    public UrgenceResponse getById(@PathVariable Long id, Authentication authentication) {
        return urgenceService.getById(id, authentication.getName());
    }

    /**
     * Board filters.
     * <p>Français : liste filtrée par statut ou priorité.</p>
     */
    @GetMapping
    public UrgenceBoardResponse list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication
    ) {
        return urgenceService.list(status, priority, sortBy, sortDir, page, size, authentication.getName());
    }

    /**
     * Triage step.
     * <p>Français : étape de triage.</p>
     */
    @PutMapping("/{id}/triage")
    public UrgenceResponse triage(
            @PathVariable Long id,
            @Valid @RequestBody TriageRequest request,
            Authentication authentication
    ) {
        return urgenceService.triage(id, request, authentication.getName());
    }

    /**
     * Orientation after workup.
     * <p>Français : orientation du patient.</p>
     */
    @PutMapping("/{id}/orient")
    public UrgenceResponse orient(
            @PathVariable Long id,
            @Valid @RequestBody OrientRequest request,
            Authentication authentication
    ) {
        return urgenceService.orient(id, request, authentication.getName());
    }

    /**
     * Terminal closure.
     * <p>Français : clôture du passage.</p>
     */
    @PutMapping("/{id}/close")
    public UrgenceResponse close(
            @PathVariable Long id,
            @RequestBody(required = false) CloseRequest request,
            Authentication authentication
    ) {
        return urgenceService.close(id, request, authentication.getName());
    }

    /**
     * Immutable audit events.
     * <p>Français : fil d'événements (audit).</p>
     */
    @GetMapping("/{id}/timeline")
    public List<UrgenceTimelineEventResponse> timeline(@PathVariable Long id, Authentication authentication) {
        return urgenceService.timeline(id, authentication.getName());
    }
}
