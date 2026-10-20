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
}