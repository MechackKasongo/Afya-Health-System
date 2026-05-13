package com.afya.afya_health_system.soa.admission.dto;

public record AdmissionClinicalFormResponse(
        Long id,
        Long admissionId,
        String antecedentsText,
        String anamnesisText,
        String physicalExamPulmonaryText,
        String physicalExamCardiacText,
        String physicalExamAbdominalText,
        String physicalExamNeurologicalText,
        String physicalExamMiscText,
        String paraclinicalText,
        String conclusionText
) {
}
