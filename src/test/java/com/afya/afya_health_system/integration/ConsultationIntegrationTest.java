package com.afya.afya_health_system.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ConsultationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createConsultationShouldReturnNotFoundWhenPatientDoesNotExist() throws Exception {
        String token = loginAndGetAccessToken();

        mockMvc.perform(post("/api/v1/consultations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": 999999,
                                  "admissionId": 1,
                                  "doctorName": "Dr K",
                                  "reason": "Controle"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Patient introuvable: 999999"));
    }

    @Test
    void createConsultationShouldReturnNotFoundWhenAdmissionDoesNotExist() throws Exception {
        String token = loginAndGetAccessToken();

        String patientBody = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPatientPayload("DOS-CNF-ADM", "cnfadm@example.com", "+243900031001")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long patientId = objectMapper.readTree(patientBody).get("id").asLong();

        mockMvc.perform(post("/api/v1/consultations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %s,
                                  "admissionId": 999999,
                                  "doctorName": "Dr Test",
                                  "reason": "Controle"
                                }
                                """.formatted(patientId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Admission introuvable: 999999"));
    }

    @Test
    void createConsultationShouldReturnConflictWhenAdmissionBelongsToAnotherPatient() throws Exception {
        String token = loginAndGetAccessToken();

        long patientAId = createPatient(token, "DOS-CNF-A", "cnfa@example.com", "+243900031011");
        long patientBId = createPatient(token, "DOS-CNF-B", "cnfb@example.com", "+243900031012");

        long admissionOtherPatientId = createAdmission(token, patientBId, "Bloc B");

        mockMvc.perform(post("/api/v1/consultations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %s,
                                  "admissionId": %s,
                                  "doctorName": "Dr Test",
                                  "reason": "Controle"
                                }
                                """.formatted(patientAId, admissionOtherPatientId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("L'admission ne correspond pas au patient indiqué"));
    }

    @Test
    void createConsultationShouldSucceedWhenAdmissionMatchesPatient() throws Exception {
        String token = loginAndGetAccessToken();

        long patientId = createPatient(token, "DOS-CNF-OK", "cnfok@example.com", "+243900031099");
        long admissionId = createAdmission(token, patientId, "Médecine interne");

        mockMvc.perform(post("/api/v1/consultations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %s,
                                  "admissionId": %s,
                                  "doctorName": "Dr OK",
                                  "reason": "Bilan"
                                }
                                """.formatted(patientId, admissionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(patientId))
                .andExpect(jsonPath("$.admissionId").value(admissionId))
                .andExpect(jsonPath("$.doctorName").value("Dr OK"));
    }

    private long createPatient(String token, String dossier, String email, String phone) throws Exception {
        String body = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPatientPayload(dossier, email, phone)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();
        assertThat(id).isPositive();
        return id;
    }

    private long createAdmission(String token, long patientId, String serviceName) throws Exception {
        String body = mockMvc.perform(post("/api/v1/admissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %s,
                                  "serviceName": "%s",
                                  "room": "R1",
                                  "bed": "1",
                                  "reason": "Test integration"
                                }
                                """.formatted(patientId, serviceName)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        long id = node.get("id").asLong();
        assertThat(id).isPositive();
        assertThat(node.get("patientId").asLong()).isEqualTo(patientId);
        return id;
    }

    private String validPatientPayload(String dossierNumber, String email, String phone) {
        return """
                {
                  "firstName": "Test",
                  "lastName": "Consult",
                  "dossierNumber": "%s",
                  "birthDate": "1990-01-15",
                  "sex": "M",
                  "phone": "%s",
                  "email": "%s",
                  "address": "Kinshasa"
                }
                """.formatted(dossierNumber, phone, email);
    }

    private String loginAndGetAccessToken() throws Exception {
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
}
