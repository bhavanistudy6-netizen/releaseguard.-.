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
        String notes,
        DeploymentType deploymentType,
        Long rollbackFromDeploymentId,
        Long rollbackTargetDeploymentId,
        String rollbackReason,
        Long deployedByUserId,
        boolean legacyCallerSupplied) {

    // Retains compatibility with existing nine-argument callers.
    public DeploymentResponse(
            Long id,
            Long applicationId,
            Long releaseId,
            String releaseVersion,
            DeploymentEnvironment environment,
            String deployedBy,
            Instant deployedAt,
            Instant recordedAt,
            String notes) {
        this(
                id,
                applicationId,
                releaseId,
                releaseVersion,
                environment,
                deployedBy,
                deployedAt,
                recordedAt,
                notes,
                DeploymentType.DEPLOY,
                null,
                null,
                null,
                null,
                true);
    }

    // Retains compatibility with existing 13-argument callers.
    public DeploymentResponse(
            Long id,
            Long applicationId,
            Long releaseId,
            String releaseVersion,
            DeploymentEnvironment environment,
            String deployedBy,
            Instant deployedAt,
            Instant recordedAt,
            String notes,
            DeploymentType deploymentType,
            Long rollbackFromDeploymentId,
            Long rollbackTargetDeploymentId,
            String rollbackReason) {
        this(
                id,
                applicationId,
                releaseId,
                releaseVersion,
                environment,
                deployedBy,
                deployedAt,
                recordedAt,
                notes,
                deploymentType,
                rollbackFromDeploymentId,
                rollbackTargetDeploymentId,
                rollbackReason,
                null,
                true);
    }

    public static DeploymentResponse from(Deployment deployment) {
        var from = deployment.getRollbackFromDeployment();
        var target = deployment.getRollbackTargetDeployment();
        Long deployedByUserId = deployment.getDeployedByUserId();
        boolean legacy = (deployedByUserId == null);

        return new DeploymentResponse(
                deployment.getId(),
                deployment.getApplication().getId(),
                deployment.getRelease().getId(),
                deployment.getRelease().getVersion(),
                deployment.getEnvironment(),
                deployment.getDeployedBy(),
                deployment.getDeployedAt(),
                deployment.getRecordedAt(),
                deployment.getNotes(),
                deployment.getDeploymentType(),
                from == null ? null : from.getId(),
                target == null ? null : target.getId(),
                deployment.getRollbackReason(),
                deployedByUserId,
                legacy);
    }
}
