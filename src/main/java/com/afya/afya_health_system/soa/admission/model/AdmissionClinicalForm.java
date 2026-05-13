package com.afya.afya_health_system.soa.admission.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Structured clinical narrative blocks for one admission (history, physical exam sections, paraclinics, conclusion).
 * <p>Français : formulaire d'hospitalisation segmenté (antécédents, anamnèse, examens par appareil, paraclinique, conclusion).</p>
 */
@Entity
@Table(
        name = "admission_clinical_forms",
        uniqueConstraints = @UniqueConstraint(name = "uk_acf_admission", columnNames = "admission_id")
)
public class AdmissionClinicalForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admission_id", nullable = false)
    private Long admissionId;

    @Lob
    @Column(name = "antecedents_text", columnDefinition = "CLOB")
    private String antecedentsText;

    @Lob
    @Column(name = "anamnesis_text", columnDefinition = "CLOB")
    private String anamnesisText;

    @Lob
    @Column(name = "physical_exam_pulmonary_text", columnDefinition = "CLOB")
    private String physicalExamPulmonaryText;

    @Lob
    @Column(name = "physical_exam_cardiac_text", columnDefinition = "CLOB")
    private String physicalExamCardiacText;

    @Lob
    @Column(name = "physical_exam_abdominal_text", columnDefinition = "CLOB")
    private String physicalExamAbdominalText;

    @Lob
    @Column(name = "physical_exam_neurological_text", columnDefinition = "CLOB")
    private String physicalExamNeurologicalText;

    @Lob
    @Column(name = "physical_exam_misc_text", columnDefinition = "CLOB")
    private String physicalExamMiscText;

    @Lob
    @Column(name = "paraclinical_text", columnDefinition = "CLOB")
    private String paraclinicalText;

    @Lob
    @Column(name = "conclusion_text", columnDefinition = "CLOB")
    private String conclusionText;

    public Long getId() {
        return id;
    }

    public Long getAdmissionId() {
        return admissionId;
    }

    public void setAdmissionId(Long admissionId) {
        this.admissionId = admissionId;
    }

    public String getAntecedentsText() {
        return antecedentsText;
    }

    public void setAntecedentsText(String antecedentsText) {
        this.antecedentsText = antecedentsText;
    }

    public String getAnamnesisText() {
        return anamnesisText;
    }

    public void setAnamnesisText(String anamnesisText) {
        this.anamnesisText = anamnesisText;
    }

    public String getPhysicalExamPulmonaryText() {
        return physicalExamPulmonaryText;
    }

    public void setPhysicalExamPulmonaryText(String physicalExamPulmonaryText) {
        this.physicalExamPulmonaryText = physicalExamPulmonaryText;
    }

    public String getPhysicalExamCardiacText() {
        return physicalExamCardiacText;
    }

    public void setPhysicalExamCardiacText(String physicalExamCardiacText) {
        this.physicalExamCardiacText = physicalExamCardiacText;
    }

    public String getPhysicalExamAbdominalText() {
        return physicalExamAbdominalText;
    }

    public void setPhysicalExamAbdominalText(String physicalExamAbdominalText) {
        this.physicalExamAbdominalText = physicalExamAbdominalText;
    }

    public String getPhysicalExamNeurologicalText() {
        return physicalExamNeurologicalText;
    }

    public void setPhysicalExamNeurologicalText(String physicalExamNeurologicalText) {
        this.physicalExamNeurologicalText = physicalExamNeurologicalText;
    }

    public String getPhysicalExamMiscText() {
        return physicalExamMiscText;
    }

    public void setPhysicalExamMiscText(String physicalExamMiscText) {
        this.physicalExamMiscText = physicalExamMiscText;
    }

    public String getParaclinicalText() {
        return paraclinicalText;
    }

    public void setParaclinicalText(String paraclinicalText) {
        this.paraclinicalText = paraclinicalText;
    }

    public String getConclusionText() {
        return conclusionText;
    }

    public void setConclusionText(String conclusionText) {
        this.conclusionText = conclusionText;
    }
}
