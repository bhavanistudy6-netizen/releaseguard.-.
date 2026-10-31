package com.releaseguard.security;

import com.releaseguard.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoleAuthorizationTest extends BaseIntegrationTest {

    // --- VIEWER TESTS ---

    @Test
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    @DisplayName("VIEWER: can read applications and releases")
    void viewerCanRead() throws Exception {
        mockMvc.perform(get("/api/v1/applications")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    @DisplayName("VIEWER: cannot create applications, releases, or deployments (403 Forbidden)")
    void viewerCannotMutate() throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"App\", \"repositoryUrl\": \"https://github.com/ex/a\", \"ownerTeam\": \"Team\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/applications/1/releases")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": \"v1.0.0\", \"gitCommitSha\": \"abc\", \"dockerImage\": \"img:1\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/applications/1/deployments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"releaseId\": 1, \"environment\": \"STAGING\", \"deployedAt\": \"2026-10-31T12:00:00Z\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    // --- DEVELOPER TESTS ---

    @Test
    @WithMockUser(username = "developer", roles = {"DEVELOPER"})
    @DisplayName("DEVELOPER: can create applications")
    void developerCanCreateApplication() throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Dev App\", \"repositoryUrl\": \"https://github.com/ex/dev\", \"ownerTeam\": \"DevTeam\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "developer", roles = {"DEVELOPER"})
    @DisplayName("DEVELOPER: cannot record deployments or access admin (403 Forbidden)")
    void developerCannotDeployOrAccessAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/applications/1/deployments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"releaseId\": 1, \"environment\": \"STAGING\", \"deployedAt\": \"2026-10-31T12:00:00Z\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    // --- REVIEWER TESTS ---

    @Test
    @WithMockUser(username = "reviewer", roles = {"REVIEWER"})
    @DisplayName("REVIEWER: cannot create applications or record deployments (403 Forbidden)")
    void reviewerCannotCreateAppsOrDeploy() throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Reviewer App\", \"repositoryUrl\": \"https://github.com/ex/rev\", \"ownerTeam\": \"Team\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/applications/1/deployments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"releaseId\": 1, \"environment\": \"STAGING\", \"deployedAt\": \"2026-10-31T12:00:00Z\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    // --- DEPLOYER TESTS ---

    @Test
    @WithMockUser(username = "deployer", roles = {"DEPLOYER"})
    @DisplayName("DEPLOYER: cannot create applications or access admin (403 Forbidden)")
    void deployerCannotCreateAppsOrAccessAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Dep App\", \"repositoryUrl\": \"https://github.com/ex/dep\", \"ownerTeam\": \"Team\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    // --- ADMIN TESTS ---

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("ADMIN: has access to admin endpoints and applications")
    void adminHasFullAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/applications")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
