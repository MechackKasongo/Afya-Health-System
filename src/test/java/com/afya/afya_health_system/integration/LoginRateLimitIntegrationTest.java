package com.afya.afya_health_system.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
                "app.security.login-rate-limit.enabled=true",
                "app.security.login-rate-limit.requests-per-minute=2",
                "app.security.login-rate-limit.refill-duration-minutes=1",
                "app.security.login-rate-limit.trust-forwarded-for-header=false",
        })
@AutoConfigureMockMvc
class LoginRateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String LOGIN_JSON = """
            {
              "username": "admin",
              "password": "Admin@12345"
            }
            """;

    @Test
    void thirdLoginWithinBudgetShouldReturn429() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .andExpect(status().isOk());

        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("429");
    }
}
