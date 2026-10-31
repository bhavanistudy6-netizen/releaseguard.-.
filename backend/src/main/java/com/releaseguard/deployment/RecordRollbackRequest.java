package com.releaseguard.deployment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record RecordRollbackRequest(
        @NotNull
        DeploymentEnvironment environment,

        @NotNull
        @Positive
        Long expectedCurrentDeploymentId,

        @NotNull
        @Positive
        Long targetDeploymentId,

        @Size(max = 150)
        String deployedBy,

        @NotNull
        @PastOrPresent
        Instant deployedAt,

        @NotBlank
        @Size(max = 5000)
        String reason
) {
}
