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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AdmissionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAdmissionShouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/admissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": 1,
                                  "serviceName": "Medecine",
                                  "room": "A101",
                                  "bed": "1",
                                  "reason": "Fievre"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void suggestBedShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admissions/suggestions/bed").param("serviceName", "Medecine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void suggestBedShouldReturnProposalForActiveService() throws Exception {
        String token = loginAndGetAccessToken();
        mockMvc.perform(get("/api/v1/admissions/suggestions/bed")
                        .header("Authorization", "Bearer " + token)
                        .param("serviceName", "Medecine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.room").value("101"))
                .andExpect(jsonPath("$.bed").value("A"))
                .andExpect(jsonPath("$.occupiedBeds").value(0))
                .andExpect(jsonPath("$.bedCapacity").value(30));
    }

    @Test
    void createTransferAndMovementsShouldWork() throws Exception {
        String token = loginAndGetAccessToken();

        long patientId = createPatient(token, "DOS-ADM-GH1", "admogh1@example.com", "+243900011001");

        String createResponse = mockMvc.perform(post("/api/v1/admissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %s,
                                  "serviceName": "Medecine",
                                  "room": "A101",
                                  "bed": "1",
                                  "reason": "Observation"
                                }
                                """.formatted(patientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_COURS"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long admissionId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(put("/api/v1/admissions/{id}/transfer", admissionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toService": "Cardiologie",
                                  "room": "B202",
                                  "bed": "2",
                                  "note": "Transfert interne"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TRANSFERE"))
                .andExpect(jsonPath("$.serviceName").value("Cardiologie"));

        mockMvc.perform(get("/api/v1/admissions/{id}/movements", admissionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("ADMISSION"))
                .andExpect(jsonPath("$[1].type").value("TRANSFERT"));
    }

    @Test
    void transferAfterDischargeShouldReturnConflict() throws Exception {
        String token = loginAndGetAccessToken();

        long patientId = createPatient(token, "DOS-ADM-GH2", "admogh2@example.com", "+243900011002");

        String createResponse = mockMvc.perform(post("/api/v1/admissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %s,
                                  "serviceName": "Chirurgie",
                                  "room": "C301",
                                  "bed": "3",
                                  "reason": "Controle"
                                }
                                """.formatted(patientId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long admissionId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(put("/api/v1/admissions/{id}/discharge", admissionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Sortie valide\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SORTI"))
                .andExpect(jsonPath("$.room").value(nullValue()))
                .andExpect(jsonPath("$.bed").value(nullValue()));

        mockMvc.perform(put("/api/v1/admissions/{id}/transfer", admissionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toService": "Reanimation",
                                  "room": "R1",
                                  "bed": "1",
                                  "note": "Doit echouer"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void declareDeathShouldClearRoomAndBed() throws Exception {
        String token = loginAndGetAccessToken();

        long patientId = createPatient(token, "DOS-ADM-DEATH", "admdeath@example.com", "+243900011098");

        String createResponse = mockMvc.perform(post("/api/v1/admissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %s,
                                  "serviceName": "Medecine",
                                  "room": "A501",
                                  "bed": "2",
                                  "reason": "UHCD"
                                }
                                """.formatted(patientId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long admissionId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(put("/api/v1/admissions/{id}/declare-death", admissionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Deces rapporte au service\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECEDE"))
                .andExpect(jsonPath("$.room").value(nullValue()))
                .andExpect(jsonPath("$.bed").value(nullValue()));
    }

    @Test
    void patientAndMedicalWritesBlockedAfterDeclareDeath() throws Exception {
        String token = loginAndGetAccessToken();

        long patientId = createPatient(token, "DOS-DEC-BLOCK", "decblock@example.com", "+243900011199");
        // Medical folder is opened automatically when the patient is created (PatientService#create).

        String createResponse = mockMvc.perform(post("/api/v1/admissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %s,
                                  "serviceName": "Medecine",
                                  "room": "A502",
                                  "bed": "4",
                                  "reason": "UHCD"
                                }
                                """.formatted(patientId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long admissionId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(put("/api/v1/admissions/{id}/declare-death", admissionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Deces rapporte au service\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECEDE"));

        mockMvc.perform(get("/api/v1/patients/{id}", patientId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deceasedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/medical-records/{patientId}", patientId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientDeceasedAt").isNotEmpty());

        mockMvc.perform(put("/api/v1/medical-records/{patientId}/allergies", patientId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Pollen\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Patient decede : modification ou nouvelle saisie non autorisee."));

        mockMvc.perform(put("/api/v1/patients/{id}", patientId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Patient",
                                  "lastName": "AdmissionTestUpdated",
                                  "birthDate": "1991-06-06",
                                  "sex": "M",
                                  "phone": "+243900011199",
                                  "email": "decblock@example.com",
                                  "address": "Kinshasa"
                                }
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/v1/admissions/{id}/declare-death", admissionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Repetition impossible\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Deces deja enregistre pour ce patient."));
    }

    @Test
    void createAdmissionShouldReturnNotFoundWhenPatientDoesNotExist() throws Exception {
        String token = loginAndGetAccessToken();

        mockMvc.perform(post("/api/v1/admissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": 999999,
                                  "serviceName": "Medecine",
                                  "room": "A101",
                                  "bed": "1",
                                  "reason": "Fievre"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Patient introuvable: 999999"));
    }

    private long createPatient(String token, String dossierNumber, String email, String phone) throws Exception {
        String createdBody = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPatientPayload(dossierNumber, email, phone)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = objectMapper.readTree(createdBody).get("id").asLong();
        assertThat(id).isPositive();
        return id;
    }

    private String validPatientPayload(String dossierNumber, String email, String phone) {
        return """
                {
                  "firstName": "Patient",
                  "lastName": "AdmissionTest",
                  "dossierNumber": "%s",
                  "birthDate": "1991-06-06",
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
