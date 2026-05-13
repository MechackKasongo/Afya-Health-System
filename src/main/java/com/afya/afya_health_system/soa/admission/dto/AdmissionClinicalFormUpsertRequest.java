package com.afya.afya_health_system.soa.admission.dto;

import jakarta.validation.constraints.Size;

/**
 * Replaces or creates the whole structured form in one request (no partial patch).
 * <p>Français : remplace ou crée le formulaire clinique structuré en une seule requête (pas de patch partiel).</p>
 */
public record AdmissionClinicalFormUpsertRequest(
        @Size(max = 50000) String antecedentsText,
        @Size(max = 50000) String anamnesisText,
        @Size(max = 50000) String physicalExamPulmonaryText,
        @Size(max = 50000) String physicalExamCardiacText,
        @Size(max = 50000) String physicalExamAbdominalText,
        @Size(max = 50000) String physicalExamNeurologicalText,
        @Size(max = 50000) String physicalExamMiscText,
        @Size(max = 50000) String paraclinicalText,
        @Size(max = 50000) String conclusionText
) {
}
