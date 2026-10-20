package com.releaseguard.deployment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DeploymentControllerTest {

    private static final String URL = "/api/v1/applications/1/deployments";

    private DeploymentService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(DeploymentService.class);

        mvc = MockMvcBuilders
                .standaloneSetup(new DeploymentController(service))
                .build();
    }

    @Test
    void recordingReturnsCreatedResponse() throws Exception {
        when(service.record(eq(1L), any(RecordDeploymentRequest.class)))
                .thenReturn(response());

        mvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "releaseId": 1,
                          "environment": "STAGING",
                          "deployedBy": "Planner Team",
                          "deployedAt": "2026-10-18T05:00:00Z",
                          "notes": "Synthetic deployment"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.releaseVersion").value("v1.0.0"))
                .andExpect(jsonPath("$.environment").value("STAGING"));

        verify(service).record(
                eq(1L),
                any(RecordDeploymentRequest.class));
    }

    @Test
    void blankDeployerReturnsBadRequest() throws Exception {
        mvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "releaseId": 1,
                          "environment": "STAGING",
                          "deployedBy": "",
                          "deployedAt": "2026-10-18T05:00:00Z"
                        }
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void currentDeploymentReturnsRecordedRelease() throws Exception {
        when(service.getCurrent(1L, DeploymentEnvironment.STAGING))
                .thenReturn(response());

        mvc.perform(get(URL + "/current")
                .param("environment", "STAGING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.releaseId").value(1));

        verify(service).getCurrent(1L, DeploymentEnvironment.STAGING);
    }

    @Test
    void historyUsesDefaultPagination() throws Exception {
        when(service.history(
                1L,
                DeploymentEnvironment.STAGING,
                0,
                10)).thenReturn(new DeploymentPageResponse(
                        List.of(response()),
                        0,
                        10,
                        1L,
                        1));

        mvc.perform(get(URL).param("environment", "STAGING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(2));

        verify(service).history(
                1L,
                DeploymentEnvironment.STAGING,
                0,
                10);
    }

    @Test
    void unknownEnvironmentReturnsBadRequest() throws Exception {
        mvc.perform(get(URL + "/current")
                .param("environment", "UNKNOWN"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void missingCurrentDeploymentReturnsNotFound() throws Exception {
        when(service.getCurrent(1L, DeploymentEnvironment.PRODUCTION))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No current deployment"));

        mvc.perform(get(URL + "/current")
                .param("environment", "PRODUCTION"))
                .andExpect(status().isNotFound());
    }

    private DeploymentResponse response() {
        Instant timestamp = Instant.parse("2026-10-18T05:00:00Z");

        return new DeploymentResponse(
                2L,
                1L,
                1L,
                "v1.0.0",
                DeploymentEnvironment.STAGING,
                "Planner Team",
                timestamp,
                timestamp,
                "Synthetic deployment");
    }
}