package com.releaseguard.deployment;

import java.time.Instant;

public record DeploymentResponse(
        Long id,
        Long applicationId,
        Long releaseId,
        String releaseVersion,
        DeploymentEnvironment environment,
        String deployedBy,
        Instant deployedAt,
        Instant recordedAt,
        String notes) {
    public static DeploymentResponse from(Deployment deployment) {
        return new DeploymentResponse(
                deployment.getId(),
                deployment.getApplication().getId(),
                deployment.getRelease().getId(),
                deployment.getRelease().getVersion(),
                deployment.getEnvironment(),
                deployment.getDeployedBy(),
                deployment.getDeployedAt(),
                deployment.getRecordedAt(),
                deployment.getNotes());
    }
}