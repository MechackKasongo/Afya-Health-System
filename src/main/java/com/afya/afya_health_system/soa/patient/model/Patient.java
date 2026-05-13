package com.afya.afya_health_system.soa.patient.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(nullable = false, length = 80)
    private String lastName;

    @Column(nullable = false, unique = true, length = 40)
    private String dossierNumber;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 10)
    private String sex;

    @Column(length = 120)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(length = 255)
    private String address;

    /** Post-nom (middle / secondary family name) — common on Congolese administrative forms. */
    @Column(name = "post_name", length = 120)
    private String postName;

    @Column(length = 120)
    private String employer;

    /** Employer / institutional registration number (matricule). */
    @Column(name = "employee_id", length = 80)
    private String employeeId;

    @Column(length = 120)
    private String profession;

    @Column(name = "spouse_name", length = 120)
    private String spouseName;

    @Column(name = "spouse_profession", length = 120)
    private String spouseProfession;

    /** Renseigne lors du premier enregistrement du décès (séjour); verrou métier lecture/écriture côté services. */
    @Column(name = "deceased_at")
    private LocalDateTime deceasedAt;

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDossierNumber() {
        return dossierNumber;
    }

    public void setDossierNumber(String dossierNumber) {
        this.dossierNumber = dossierNumber;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPostName() {
        return postName;
    }

    public void setPostName(String postName) {
        this.postName = postName;
    }

    public String getEmployer() {
        return employer;
    }

    public void setEmployer(String employer) {
        this.employer = employer;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public String getSpouseName() {
        return spouseName;
    }

    public void setSpouseName(String spouseName) {
        this.spouseName = spouseName;
    }

    public String getSpouseProfession() {
        return spouseProfession;
    }

    public void setSpouseProfession(String spouseProfession) {
        this.spouseProfession = spouseProfession;
    }

    public LocalDateTime getDeceasedAt() {
        return deceasedAt;
    }

    public void setDeceasedAt(LocalDateTime deceasedAt) {
        this.deceasedAt = deceasedAt;
    }
}
