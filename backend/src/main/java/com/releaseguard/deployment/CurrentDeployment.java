package com.releaseguard.deployment;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "current_deployments")
public class CurrentDeployment {

    @EmbeddedId
    private CurrentDeploymentId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deployment_id", nullable = false)
    private Deployment deployment;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected CurrentDeployment() {
    }

    public CurrentDeployment(Deployment deployment) {
        this.id = new CurrentDeploymentId(
                deployment.getApplication().getId(),
                deployment.getEnvironment());

        this.deployment = deployment;
    }

    public void changeDeployment(Deployment replacement) {
        if (!Objects.equals(
                id.getApplicationId(),
                replacement.getApplication().getId()) || id.getEnvironment() != replacement.getEnvironment()) {
            throw new IllegalArgumentException(
                    "Replacement must belong to the same application "
                            + "and environment");
        }

        this.deployment = replacement;
    }

    @PrePersist
    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    public CurrentDeploymentId getId() {
        return id;
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}