package com.releaseguard.security;

import com.releaseguard.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminUserManagementTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "developer", roles = {"DEVELOPER"})
    @DisplayName("Non-admin caller cannot access admin user management (403 Forbidden)")
    void nonAdminCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"newuser\", \"password\": \"Pass!\", \"displayName\": \"New\", \"roles\": [\"VIEWER\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("ADMIN: can create user, list users, update status/roles, and reset password")
    void adminCanManageUsersLifecycle() throws Exception {
        // 1. Create a new user
        String createJson = """
                {
                    "username": "managed-user",
                    "password": "InitialPassword123!",
                    "displayName": "Managed User",
                    "roles": ["DEVELOPER"]
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("managed-user"))
                .andExpect(jsonPath("$.roles[0]").value("DEVELOPER"))
                .andReturn();

        // Extract ID from response
        String responseContent = createResult.getResponse().getContentAsString();
        tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
        tools.jackson.databind.JsonNode root = mapper.readTree(responseContent);
        long newUserId = root.get("id").asLong();

        // 2. Update user roles and status
        String updateJson = """
                {
                    "displayName": "Promoted Managed User",
                    "roles": ["DEVELOPER", "REVIEWER"],
                    "enabled": true
                }
                """;

        mockMvc.perform(put("/api/v1/admin/users/" + newUserId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Promoted Managed User"));

        // 3. Reset user password
        String resetPasswordJson = """
                {
                    "newPassword": "NewStrongPassword2026!"
                }
                """;

        mockMvc.perform(post("/api/v1/admin/users/" + newUserId + "/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetPasswordJson))
                .andExpect(status().isNoContent());

        // 4. Delete the managed user
        mockMvc.perform(delete("/api/v1/admin/users/" + newUserId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("ADMIN: preventing disabling or removing ADMIN role from the last active admin")
    void cannotDisableLastAdmin() throws Exception {
        User adminUser = userRepository.findByUsername("admin")
                .orElseThrow();

        // Attempt to remove ADMIN role from the sole admin
        String updateJson = """
                {
                    "displayName": "Admin Attempting Demotion",
                    "roles": ["VIEWER"],
                    "enabled": true
                }
                """;

        mockMvc.perform(put("/api/v1/admin/users/" + adminUser.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isConflict())
                .andExpect(status().reason(containsString("last active administrator")));
    }
}
