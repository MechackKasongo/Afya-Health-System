package com.afya.afya_health_system.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de données H2 dédiée pour ne pas verrouiller l’admin utilisé par les autres intégrations.
 */
@SpringBootTest
@TestPropertySource(
        properties = {
                "spring.datasource.url=jdbc:h2:mem:afya_lockout_integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "app.security.login-lockout.enabled=true",
                "app.security.login-lockout.max-attempts=3",
                "app.security.login-lockout.lock-duration-minutes=60",
        }
)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class LoginLockoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private void postWrongLogin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"incorrect"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiants invalides"));
    }

    @Test
    void shouldLockAccountAfterRepeatedFailures() throws Exception {
        postWrongLogin();
        postWrongLogin();
        postWrongLogin();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Admin@12345"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("verrouill")))
                .andExpect(jsonPath("$.message", containsString("administrateur")));
    }
}
