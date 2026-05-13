package com.afya.afya_health_system.soa.patient.model;

import jakarta.persistence.*;

@Entity
@Table(name = "patient_dossier_sequences")
public class PatientDossierSequence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sequence_year", nullable = false, unique = true)
    private Integer sequenceYear;

    @Column(nullable = false, length = 4)
    private String letterBlock;

    @Column(nullable = false)
    private Integer sequenceNumber;

    public Long getId() {
        return id;
    }

    public Integer getSequenceYear() {
        return sequenceYear;
    }

    public void setSequenceYear(Integer sequenceYear) {
        this.sequenceYear = sequenceYear;
    }

    public String getLetterBlock() {
        return letterBlock;
    }

    public void setLetterBlock(String letterBlock) {
        this.letterBlock = letterBlock;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
}
