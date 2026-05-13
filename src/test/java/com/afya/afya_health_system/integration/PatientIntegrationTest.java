package com.afya.afya_health_system.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PatientIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createPatientShouldReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPatientPayload("DOS-001", "alice@example.com", "+243900000001")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createAndGetPatientShouldWorkWithValidToken() throws Exception {
        String token = loginAndGetAccessToken();

        String createdBody =         mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPatientPayload("DOS-100", "patient100@example.com", "+243900000100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.dossierNumber").value("DOS-100"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long patientId = objectMapper.readTree(createdBody).get("id").asLong();
        assertThat(patientId).isPositive();

        mockMvc.perform(get("/api/v1/patients/{id}", patientId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientId))
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.lastName").value("Martin"));
    }

    @Test
    void createPatientShouldReturnBadRequestWhenEmailInvalid() throws Exception {
        String token = loginAndGetAccessToken();

        mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPatientPayload("DOS-200", "email-invalide", "+243900000200")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createPatientShouldReturnConflictWhenDossierNumberAlreadyExists() throws Exception {
        String token = loginAndGetAccessToken();
        String payload = validPatientPayload("DOS-300", "patient300@example.com", "+243900000300");

        mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
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

    private String validPatientPayload(String dossierNumber, String email, String phone) {
        return """
                {
                  "firstName": "Alice",
                  "lastName": "Martin",
                  "dossierNumber": "%s",
                  "birthDate": "1990-05-10",
                  "sex": "F",
                  "phone": "%s",
                  "email": "%s",
                  "address": "Kinshasa"
                }
                """.formatted(dossierNumber, phone, email);
    }
}
