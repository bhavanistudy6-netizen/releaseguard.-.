package com.releaseguard.deployment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RollbackControllerTest {

    private static final String URL = "/api/v1/applications/1/deployments/rollbacks";

    @Mock
    private RollbackService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders
                .standaloneSetup(new RollbackController(service))
                .build();
    }

    @Test
    void returns201WithRollbackDetails() throws Exception {
        Instant now = Instant.now();

        DeploymentResponse deployment = new DeploymentResponse(
                101L,
                1L,
                5L,
                "v1.0.0",
                DeploymentEnvironment.STAGING,
                "Deploy Team",
                now.minusSeconds(5),
                now,
                null,
                DeploymentType.ROLLBACK,
                100L,
                90L,
                "Restore previous release");

        when(service.record(
                eq(1L),
                any(RecordRollbackRequest.class)))
                .thenReturn(new RollbackResponse(
                        deployment,
                        DeploymentType.ROLLBACK,
                        100L,
                        90L,
                        "Restore previous release"));

        mvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deployment.id").value(101))
                .andExpect(jsonPath("$.deployment.releaseId").value(5))
                .andExpect(jsonPath("$.deploymentType").value("ROLLBACK"))
                .andExpect(jsonPath("$.rollbackFromDeploymentId").value(100))
                .andExpect(jsonPath("$.rollbackTargetDeploymentId").value(90))
                .andExpect(jsonPath("$.reason")
                        .value("Restore previous release"));

        verify(service).record(
                eq(1L),
                any(RecordRollbackRequest.class));
    }

    @Test
    void returns409WhenCurrentDeploymentHasChanged() throws Exception {
        when(service.record(
                eq(1L),
                any(RecordRollbackRequest.class)))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Current deployment has changed"));

        mvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
                .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "blankReason",
            "missingTarget",
            "unknownEnvironment",
            "futureTimestamp"
    })
    void rejectsInvalidRequestsBeforeCallingService(String scenario)
            throws Exception {

        String valid = validBody();

        String invalid = switch (scenario) {
            case "blankReason" -> valid.replace(
                    "\"Restore previous release\"",
                    "\"   \"");

            case "missingTarget" -> valid.replace(
                    "\"targetDeploymentId\":90",
                    "\"targetDeploymentId\":null");

            case "unknownEnvironment" -> valid.replace(
                    "\"STAGING\"",
                    "\"INVALID\"");

            case "futureTimestamp" -> valid.replace(
                    "\"deployedAt\":\"" + DEPLOYED_AT + "\"",
                    "\"deployedAt\":\""
                            + Instant.now().plusSeconds(86400)
                            + "\"");

            default -> throw new IllegalArgumentException(scenario);
        };

        mvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalid))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    private static final String DEPLOYED_AT = "2026-10-18T05:00:00Z";

    private String validBody() {
        return """
                {
                  "environment":"STAGING",
                  "expectedCurrentDeploymentId":100,
                  "targetDeploymentId":90,
                  "deployedBy":"Deploy Team",
                  "deployedAt":"%s",
                  "reason":"Restore previous release"
                }
                """.formatted(DEPLOYED_AT);
    }
}