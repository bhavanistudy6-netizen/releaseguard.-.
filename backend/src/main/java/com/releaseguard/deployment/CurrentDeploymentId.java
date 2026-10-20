package com.releaseguard.deployment;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CurrentDeploymentId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeploymentEnvironment environment;

    protected CurrentDeploymentId() {
    }

    public CurrentDeploymentId(
            Long applicationId,
            DeploymentEnvironment environment) {
        this.applicationId = applicationId;
        this.environment = environment;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public DeploymentEnvironment getEnvironment() {
        return environment;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof CurrentDeploymentId that)) {
            return false;
        }

        return Objects.equals(applicationId, that.applicationId)
                && environment == that.environment;
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationId, environment);
    }
}