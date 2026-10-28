package com.releaseguard.deployment;

import com.releaseguard.application.Application;
import com.releaseguard.release.Release;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "deployments")
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "release_id", nullable = false)
    private Release release;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeploymentEnvironment environment;

    @Column(name = "deployed_by", nullable = false, length = 150)
    private String deployedBy;

    @Column(name = "deployed_at", nullable = false)
    private Instant deployedAt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "deployment_type", nullable = false, length = 20)
    private DeploymentType deploymentType = DeploymentType.DEPLOY;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rollback_from_deployment_id")
    private Deployment rollbackFromDeployment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rollback_target_deployment_id")
    private Deployment rollbackTargetDeployment;

    @Column(name = "rollback_reason", columnDefinition = "TEXT")
    private String rollbackReason;

    protected Deployment() {
    }

    public Deployment(
            Release release,
            DeploymentEnvironment environment,
            String deployedBy,
            Instant deployedAt,
            String notes) {
        this.application = release.getApplication();
        this.release = release;
        this.environment = environment;
        this.deployedBy = deployedBy;
        this.deployedAt = deployedAt;
        this.notes = notes;
    }

    @PrePersist
    void onCreate() {
        recordedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Application getApplication() {
        return application;
    }

    public Release getRelease() {
        return release;
    }

    public DeploymentEnvironment getEnvironment() {
        return environment;
    }

    public String getDeployedBy() {
        return deployedBy;
    }

    public Instant getDeployedAt() {
        return deployedAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public String getNotes() {
        return notes;
    }

    public static Deployment rollback(
            Deployment from,
            Deployment target,
            String deployedBy,
            Instant deployedAt,
            String reason) {

        if (!from.getApplication().getId().equals(
                target.getApplication().getId())
                || from.getEnvironment() != target.getEnvironment()) {

            throw new IllegalArgumentException(
                    "Rollback deployments must belong to the same application and environment");
        }

        if (target.getId() >= from.getId()) {
            throw new IllegalArgumentException(
                    "Rollback target must precede the current deployment");
        }

        Deployment deployment = new Deployment(
                target.getRelease(),
                target.getEnvironment(),
                deployedBy,
                deployedAt,
                null);

        deployment.deploymentType = DeploymentType.ROLLBACK;
        deployment.rollbackFromDeployment = from;
        deployment.rollbackTargetDeployment = target;
        deployment.rollbackReason = reason;

        return deployment;
    }

    public DeploymentType getDeploymentType() {
        return deploymentType;
    }

    public Deployment getRollbackFromDeployment() {
        return rollbackFromDeployment;
    }

    public Deployment getRollbackTargetDeployment() {
        return rollbackTargetDeployment;
    }

    public String getRollbackReason() {
        return rollbackReason;
    }
}