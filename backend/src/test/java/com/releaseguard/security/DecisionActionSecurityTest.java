package com.releaseguard.security;

import com.releaseguard.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DecisionActionSecurityTest extends BaseIntegrationTest {

    @Test
    @WithMockUser(username = "developer", roles = {"DEVELOPER"})
    @DisplayName("DEVELOPER: forbidden (403) from approving, rejecting, revoking, or retiring")
    void developerForbiddenFromReviewerActions() throws Exception {
        // APPROVE attempt by DEVELOPER -> 403
        mockMvc.perform(post("/api/v1/applications/1/releases/1/decisions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"APPROVE\", \"reason\": \"Attempt by dev\"}"))
                .andExpect(status().isForbidden());

        // REJECT attempt by DEVELOPER -> 403
        mockMvc.perform(post("/api/v1/applications/1/releases/1/decisions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"REJECT\", \"reason\": \"Attempt by dev\"}"))
                .andExpect(status().isForbidden());

        // REVOKE attempt by DEVELOPER -> 403
        mockMvc.perform(post("/api/v1/applications/1/releases/1/decisions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"REVOKE\", \"reason\": \"Attempt by dev\"}"))
                .andExpect(status().isForbidden());

        // RETIRE attempt by DEVELOPER -> 403
        mockMvc.perform(post("/api/v1/applications/1/releases/1/decisions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"RETIRE\", \"reason\": \"Attempt by dev\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "reviewer", roles = {"REVIEWER"})
    @DisplayName("REVIEWER: forbidden (403) from submitting releases for review")
    void reviewerForbiddenFromSubmitAction() throws Exception {
        mockMvc.perform(post("/api/v1/applications/1/releases/1/decisions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"SUBMIT\", \"reason\": \"Attempt by reviewer\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "deployer", roles = {"DEPLOYER"})
    @DisplayName("DEPLOYER: forbidden (403) from all decision actions")
    void deployerForbiddenFromDecisions() throws Exception {
        mockMvc.perform(post("/api/v1/applications/1/releases/1/decisions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"SUBMIT\", \"reason\": \"Attempt by deployer\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/applications/1/releases/1/decisions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"APPROVE\", \"reason\": \"Attempt by deployer\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    @DisplayName("VIEWER: forbidden (403) from all decision actions")
    void viewerForbiddenFromDecisions() throws Exception {
        mockMvc.perform(post("/api/v1/applications/1/releases/1/decisions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"SUBMIT\", \"reason\": \"Attempt by viewer\"}"))
                .andExpect(status().isForbidden());
    }
}
