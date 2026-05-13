package com.afya.afya_health_system.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PrescriptionClinicalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void prescriptionClinicalFormAndAdministrationFlow() throws Exception {
        String token = login();

        long patientId = createPatient(token, "DOS-PRX-01", "prx01@example.com");
        long admissionId = createAdmission(token, patientId);

        mockMvc.perform(get("/api/v1/admissions/{id}/clinical-form", admissionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admissionId").value(admissionId))
                .andExpect(jsonPath("$.antecedentsText").value(nullValue()));

        mockMvc.perform(put("/api/v1/admissions/{id}/clinical-form", admissionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "antecedentsText": "RAS",
                                  "anamnesisText": "Patient stable",
                                  "physicalExamPulmonaryText": "Echanges respiratoires symetriques",
                                  "physicalExamCardiacText": "Souffle systolique",
                                  "physicalExamAbdominalText": "Souple",
                                  "physicalExamNeurologicalText": "Vigilant",
                                  "physicalExamMiscText": null,
                                  "paraclinicalText": null,
                                  "conclusionText": "Hospitalisation"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.antecedentsText").value("RAS"))
                .andExpect(jsonPath("$.conclusionText").value("Hospitalisation"));

        mockMvc.perform(post("/api/v1/admissions/{id}/prescription-lines", admissionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "medicationName": "Paracetamol 500mg",
                                  "dosageText": "2 x 1 cp",
                                  "frequencyText": "8h-16h-22h",
                                  "prescriberName": "Dr Test",
                                  "startDate": "2026-05-01",
                                  "endDate": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicationName").value("Paracetamol 500mg"))
                .andExpect(jsonPath("$.active").value(true));

        String lineBody = mockMvc.perform(get("/api/v1/admissions/{id}/prescription-lines", admissionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long lineId = objectMapper.readTree(lineBody).get(0).get("id").asLong();

        mockMvc.perform(post("/api/v1/admissions/{aid}/prescription-lines/{lid}/administrations", admissionId, lineId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "administrationDate": "2026-05-07",
                                  "slot": "MATIN",
                                  "administered": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.administered").value(true));

        mockMvc.perform(post("/api/v1/admissions/{aid}/prescription-lines/{lid}/administrations", admissionId, lineId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "administrationDate": "2026-05-07",
                                  "slot": "MATIN",
                                  "administered": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.administered").value(false));

        mockMvc.perform(get("/api/v1/admissions/{aid}/prescription-lines/{lid}/administrations", admissionId, lineId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slot").value("MATIN"))
                .andExpect(jsonPath("$[0].administered").value(false));
    }

    private String login() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "Admin@12345"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(loginResponse).get("accessToken").asText();
    }

    private long createPatient(String token, String dossier, String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Paul",
                                  "lastName": "Rx",
                                  "dossierNumber": "%s",
                                  "birthDate": "1990-01-01",
                                  "sex": "M",
                                  "phone": "+243900000001",
                                  "email": "%s",
                                  "address": "Test"
                                }
                                """.formatted(dossier, email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long createAdmission(String token, long patientId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/admissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %s,
                                  "serviceName": "Medecine",
                                  "room": "P1",
                                  "bed": "1",
                                  "reason": "Test Rx"
                                }
                                """.formatted(patientId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }
}
