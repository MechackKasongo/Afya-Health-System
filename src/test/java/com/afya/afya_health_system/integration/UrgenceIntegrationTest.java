package com.afya.afya_health_system.integration;

import com.afya.afya_health_system.soa.hospitalservice.model.HospitalService;
import com.afya.afya_health_system.soa.identity.model.AppUser;
import com.afya.afya_health_system.soa.identity.model.Role;
import com.afya.afya_health_system.soa.identity.repository.AppUserRepository;
import com.afya.afya_health_system.soa.identity.repository.RoleRepository;
import com.afya.afya_health_system.soa.hospitalservice.repository.HospitalServiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class UrgenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private HospitalServiceRepository hospitalServiceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String CLINICAL_TEST_PASSWORD = "Test@12345";

    @Test
    void createUrgenceShouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/urgences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": 99,
                                  "motif": "Douleur thoracique",
                                  "priority": "HAUTE"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void triageOrientCloseAndTimelineShouldWork() throws Exception {
        String token = loginAndGetAccessToken();

        long patientId = createPatient(token, "DOS-URG-TL", "urg-tl@example.com", "UrgTimeline");

        String createResponse = mockMvc.perform(post("/api/v1/urgences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %s,
                                  "motif": "Dyspnee",
                                  "priority": "HAUTE"
                                }
                                """.formatted(patientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_ATTENTE_TRIAGE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long urgenceId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(put("/api/v1/urgences/{id}/triage", urgenceId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "triageLevel": "ORANGE",
                                  "details": "Signes de detresse moderee"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_COURS"));

        mockMvc.perform(put("/api/v1/urgences/{id}/orient", urgenceId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orientation": "Cardiologie",
                                  "details": "Orientation rapide"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ORIENTE"));

        mockMvc.perform(put("/api/v1/urgences/{id}/close", urgenceId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"details\":\"Prise en charge terminee\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOTURE"));

        mockMvc.perform(get("/api/v1/urgences/{id}/timeline", urgenceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("CREATED"))
                .andExpect(jsonPath("$[1].type").value("TRIAGE"))
                .andExpect(jsonPath("$[2].type").value("ORIENT"))
                .andExpect(jsonPath("$[3].type").value("CLOSED"));
    }

    @Test
    void triageAfterCloseShouldReturnConflict() throws Exception {
        String token = loginAndGetAccessToken();

        long patientId = createPatient(token, "DOS-URG-TC", "urg-tc@example.com", "UrgTcAfter");

        String createResponse = mockMvc.perform(post("/api/v1/urgences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %s,
                                  "motif": "Cefalee",
                                  "priority": "MOYENNE"
                                }
                                """.formatted(patientId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long urgenceId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(put("/api/v1/urgences/{id}/close", urgenceId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"details\":\"Cloture initiale\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/urgences/{id}/triage", urgenceId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "triageLevel": "VERT",
                                  "details": "Doit echouer"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void createUrgenceShouldReturnNotFoundWhenPatientDoesNotExist() throws Exception {
        String token = loginAndGetAccessToken();

        mockMvc.perform(post("/api/v1/urgences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": 999999,
                                  "motif": "Douleur thoracique",
                                  "priority": "HAUTE"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Patient introuvable: 999999"));
    }

    @Test
    void restrictedClinicalUserWithoutUrgencesAssignment_seesNoUrgencesAndCannotCreateOrOpen() throws Exception {
        String clinicalUsername = "med_cardio_" + UUID.randomUUID().toString().substring(0, 10);
        persistClinicalUser(clinicalUsername, "Cardiologie");

        String adminToken = loginAndGetAccessToken();
        long patientId = createPatient(adminToken, "DOS-URG-SC", "urg-scope-cardio@example.com", "ScopeCardio");

        String createBody = mockMvc.perform(post("/api/v1/urgences")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %s,
                                  "motif": "Scope test",
                                  "priority": "P2"
                                }
                                """.formatted(patientId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long existingUrgenceId = objectMapper.readTree(createBody).get("id").asLong();

        String clinicalToken = loginAndGetAccessToken(clinicalUsername, CLINICAL_TEST_PASSWORD);

        mockMvc.perform(get("/api/v1/urgences")
                        .header("Authorization", "Bearer " + clinicalToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scopeRestricted").value(true))
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/urgences/{id}", existingUrgenceId)
                        .header("Authorization", "Bearer " + clinicalToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Urgence introuvable: " + existingUrgenceId));

        mockMvc.perform(get("/api/v1/urgences/{id}/timeline", existingUrgenceId)
                        .header("Authorization", "Bearer " + clinicalToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/urgences")
                        .header("Authorization", "Bearer " + clinicalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %s,
                                  "motif": "Tentative refusee",
                                  "priority": "P3"
                                }
                                """.formatted(patientId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Accès aux urgences non autorisé pour votre affectation."));

        mockMvc.perform(put("/api/v1/urgences/{id}/triage", existingUrgenceId)
                        .header("Authorization", "Bearer " + clinicalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "triageLevel": "VERT",
                                  "details": "Doit echouer"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void clinicalUserWithUrgencesAssignment_canCreateListAndOpenUrgence() throws Exception {
        String clinicalUsername = "med_urg_" + UUID.randomUUID().toString().substring(0, 10);
        persistClinicalUser(clinicalUsername, "Urgences");

        String adminToken = loginAndGetAccessToken();
        long patientId = createPatient(adminToken, "DOS-URG-OK", "urg-scope-ok@example.com", "ScopeUrgOk");

        String clinicalToken = loginAndGetAccessToken(clinicalUsername, CLINICAL_TEST_PASSWORD);

        String createBody = mockMvc.perform(post("/api/v1/urgences")
                        .header("Authorization", "Bearer " + clinicalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": %s,
                                  "motif": "Prise en charge",
                                  "priority": "P2"
                                }
                                """.formatted(patientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_ATTENTE_TRIAGE"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long urgenceId = objectMapper.readTree(createBody).get("id").asLong();

        mockMvc.perform(get("/api/v1/urgences")
                        .header("Authorization", "Bearer " + clinicalToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scopeRestricted").value(false))
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/urgences/{id}", urgenceId)
                        .header("Authorization", "Bearer " + clinicalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(urgenceId))
                .andExpect(jsonPath("$.patientId").value(patientId));
    }

    private void persistClinicalUser(String username, String hospitalServiceCatalogName) {
        Role medecin = roleRepository.findByCode("ROLE_MEDECIN")
                .orElseThrow(() -> new IllegalStateException("ROLE_MEDECIN manquant (bootstrap)."));
        HospitalService svc = hospitalServiceRepository.findByNameIgnoreCase(hospitalServiceCatalogName)
                .orElseThrow(() -> new IllegalStateException("Service hospitalier manquant: " + hospitalServiceCatalogName));
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setFullName("Médecin périmètre test");
        user.setEmail(username + "@scope.afya.test");
        user.setPasswordHash(passwordEncoder.encode(CLINICAL_TEST_PASSWORD));
        user.setRoles(new HashSet<>(Set.of(medecin)));
        user.setHospitalServices(new HashSet<>(Set.of(svc)));
        user.setActive(true);
        user.setFailedLoginAttempts(0);
        appUserRepository.save(user);
    }

    private long createPatient(String token, String dossierNumber, String email, String lastName) throws Exception {
        String createdBody = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPatientPayload(dossierNumber, email, lastName)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = objectMapper.readTree(createdBody).get("id").asLong();
        assertThat(id).isPositive();
        return id;
    }

    private String validPatientPayload(String dossierNumber, String email, String lastName) {
        return """
                {
                  "firstName": "Bob",
                  "lastName": "%s",
                  "dossierNumber": "%s",
                  "birthDate": "1985-03-03",
                  "sex": "M",
                  "phone": "+243900000022",
                  "email": "%s",
                  "address": "Lubumbashi"
                }
                """.formatted(lastName, dossierNumber, email);
    }

    private String loginAndGetAccessToken() throws Exception {
        return loginAndGetAccessToken("admin", "Admin@12345");
    }

    private String loginAndGetAccessToken(String username, String password) throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(loginResponse).get("accessToken").asText();
    }
}
