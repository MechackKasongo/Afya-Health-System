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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class VitalSignIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listVitalSignsShouldReturnNotFoundWhenAdmissionDoesNotExist() throws Exception {
        String token = loginAndGetAccessToken();

        mockMvc.perform(get("/api/v1/admissions/{id}/vital-signs", 999999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Admission introuvable: 999999"));
    }

    @Test
    void createAndListVitalSignsShouldWork() throws Exception {
        String token = loginAndGetAccessToken();

        String patientBody = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientPayload("DOS-VS-01", "vs01@example.com")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long patientId = objectMapper.readTree(patientBody).get("id").asLong();

        String admissionBody = mockMvc.perform(post("/api/v1/admissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %s,
                                  "serviceName": "Medecine",
                                  "room": "V1",
                                  "bed": "1",
                                  "reason": "Surveillance"
                                }
                                """.formatted(patientId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long admissionId = objectMapper.readTree(admissionBody).get("id").asLong();

        String isoNow = LocalDateTime.now().withNano(0).toString();

        mockMvc.perform(post("/api/v1/admissions/{id}/vital-signs", admissionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recordedAt": "%s",
                                  "slot": "MATIN",
                                  "systolicBp": 120,
                                  "diastolicBp": 80,
                                  "pulseBpm": 72,
                                  "temperatureCelsius": 36.8,
                                  "weightKg": 70.5,
                                  "diuresisMl": 800,
                                  "stoolsNote": "Normales"
                                }
                                """.formatted(isoNow)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admissionId").value(admissionId))
                .andExpect(jsonPath("$.systolicBp").value(120))
                .andExpect(jsonPath("$.temperatureCelsius").value(36.8));

        String listBody = mockMvc.perform(get("/api/v1/admissions/{id}/vital-signs", admissionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode arr = objectMapper.readTree(listBody);
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isEqualTo(1);
        assertThat(arr.get(0).get("pulseBpm").asInt()).isEqualTo(72);
    }

    @Test
    void createPatientWithExtendedDemographicsShouldExposeFields() throws Exception {
        String token = loginAndGetAccessToken();

        String body = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Marie",
                                  "lastName": "Kabila",
                                  "dossierNumber": "DOS-DEMO-EXT",
                                  "birthDate": "1995-08-20",
                                  "sex": "F",
                                  "phone": "+243911111111",
                                  "email": "demo.ext@example.com",
                                  "address": "Lubumbashi",
                                  "postName": "Mpoyi",
                                  "employer": "CHU Jason Sendwe",
                                  "employeeId": "EMP-100",
                                  "profession": "Infirmiere",
                                  "spouseName": "Jean Dupont",
                                  "spouseProfession": "Médecin"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postName").value("Mpoyi"))
                .andExpect(jsonPath("$.employer").value("CHU Jason Sendwe"))
                .andExpect(jsonPath("$.employeeId").value("EMP-100"))
                .andExpect(jsonPath("$.profession").value("Infirmiere"))
                .andExpect(jsonPath("$.spouseName").value("Jean Dupont"))
                .andExpect(jsonPath("$.spouseProfession").value("Médecin"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/v1/patients/{id}/administrative-summary", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Marie Kabila Mpoyi"))
                .andExpect(jsonPath("$.employer").value("CHU Jason Sendwe"));
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

    private String patientPayload(String dossierNumber, String email) {
        return """
                {
                  "firstName": "Test",
                  "lastName": "Vitals",
                  "dossierNumber": "%s",
                  "birthDate": "2000-01-01",
                  "sex": "M",
                  "phone": "+243900000099",
                  "email": "%s",
                  "address": "Test"
                }
                """.formatted(dossierNumber, email);
    }
}
