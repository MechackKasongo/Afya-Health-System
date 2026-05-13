package com.afya.afya_health_system.soa.admission.dto;

/** Proposition de chambre / lit pour une admission selon la capacité du service et l'occupation courante. */
public record BedSuggestionResponse(
        boolean available,
        String room,
        String bed,
        long occupiedBeds,
        int bedCapacity,
        String message
) {
}
