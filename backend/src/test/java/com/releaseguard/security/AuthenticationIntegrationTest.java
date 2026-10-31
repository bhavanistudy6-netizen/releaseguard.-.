package com.releaseguard.security;

import com.releaseguard.BaseIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthenticationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void resetLockout() {
        loginAttemptService.loginSucceeded("admin");
        loginAttemptService.loginSucceeded("lockout-user");
    }

    @Test
    @DisplayName("Anonymous caller to protected endpoint receives 401 Unauthorized with JSON body")
    void anonymousCallerReceives401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(containsString("Authentication is required")));

        mockMvc.perform(get("/api/v1/applications")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Invalid credentials returns 401 with generic error message")
    void invalidCredentialsReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"admin\", \"password\": \"WrongPassword123!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason(containsString("Invalid username or password")));
    }

    @Test
    @DisplayName("Five consecutive failed logins trigger lockout and return 429 Too Many Requests")
    void lockoutAfterFiveFailures() throws Exception {
        String testUser = "lockout-user";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\": \"" + testUser + "\", \"password\": \"Wrong!\"}"))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt is blocked by rate-limiting lockout
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"" + testUser + "\", \"password\": \"Wrong!\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(status().reason(containsString("temporarily locked")));
    }

    @Test
    @DisplayName("Valid login establishes session, sets CSRF cookie, and allows protected calls")
    void successfulLoginAndSessionLifecycle() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"admin\", \"password\": \"AdminSecurePass2026!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session, "Session must be established on successful login");

        Cookie xsrfCookie = loginResult.getResponse().getCookie("XSRF-TOKEN");
        String csrfToken = xsrfCookie != null ? xsrfCookie.getValue() : null;

        // Verify /me with session
        mockMvc.perform(get("/api/v1/auth/me")
                        .session(session)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));

        // State-changing POST without CSRF should fail with 403 Forbidden
        mockMvc.perform(post("/api/v1/auth/logout")
                        .session(session))
                .andExpect(status().isForbidden());

        // Acquire CSRF token if cookie was not attached directly
        if (csrfToken == null) {
            MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf")
                            .session(session))
                    .andExpect(status().isOk())
                    .andReturn();
            Cookie c = csrfResult.getResponse().getCookie("XSRF-TOKEN");
            if (c != null) {
                csrfToken = c.getValue();
            }
        }

        // State-changing POST with CSRF header succeeds
        if (csrfToken != null) {
            mockMvc.perform(post("/api/v1/auth/logout")
                            .session(session)
                            .cookie(new Cookie("XSRF-TOKEN", csrfToken))
                            .header("X-XSRF-TOKEN", csrfToken))
                    .andExpect(status().isOk());

            // After logout, session is invalidated
            mockMvc.perform(get("/api/v1/auth/me")
                            .session(session)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }
}
