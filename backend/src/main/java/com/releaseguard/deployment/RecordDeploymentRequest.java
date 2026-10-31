package com.releaseguard.deployment;

import jakarta.validation.constraints.*;

import java.time.Instant;

public record RecordDeploymentRequest(

        @NotNull @Positive Long releaseId,

        @NotNull DeploymentEnvironment environment,

        @Size(max = 150) String deployedBy,

        @NotNull @PastOrPresent Instant deployedAt,

        @Size(max = 5000) String notes) {
}
