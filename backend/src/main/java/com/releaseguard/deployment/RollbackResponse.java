package com.releaseguard.deployment;

public record RollbackResponse(
        DeploymentResponse deployment,
        DeploymentType deploymentType,
        Long rollbackFromDeploymentId,
        Long rollbackTargetDeploymentId,
        String reason) {
    public static RollbackResponse from(Deployment deployment) {
        return new RollbackResponse(
                DeploymentResponse.from(deployment),
                deployment.getDeploymentType(),
                deployment.getRollbackFromDeployment().getId(),
                deployment.getRollbackTargetDeployment().getId(),
                deployment.getRollbackReason());
    }
}