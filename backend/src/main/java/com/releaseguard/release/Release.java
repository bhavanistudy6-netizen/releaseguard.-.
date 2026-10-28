package com.releaseguard.release;

import com.releaseguard.application.Application;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "releases")
public class Release {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(nullable = false, length = 100)
    private String version;

    @Column(name = "git_commit_sha", nullable = false, length = 64)
    private String gitCommitSha;

    @Column(name = "docker_image", nullable = false, length = 500)
    private String dockerImage;

    @Column(name = "image_digest", length = 100)
    private String imageDigest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReleaseStatus status = ReleaseStatus.DRAFT;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Release() {
    }

    public Release(
            Application application,
            String version,
            String gitCommitSha,
            String dockerImage,
            String imageDigest) {
        this.application = application;
        this.version = version;
        this.gitCommitSha = gitCommitSha;
        this.dockerImage = dockerImage;
        this.imageDigest = imageDigest;
    }

    public void submitForReview() {
        requireStatus(ReleaseStatus.DRAFT);
        status = ReleaseStatus.READY_FOR_REVIEW;
    }

    public void approve() {
        requireStatus(ReleaseStatus.READY_FOR_REVIEW);
        status = ReleaseStatus.APPROVED;
    }

    public void reject() {
        requireStatus(ReleaseStatus.READY_FOR_REVIEW);
        status = ReleaseStatus.DRAFT;
    }

    public void revokeApproval() {
        requireStatus(ReleaseStatus.APPROVED);
        status = ReleaseStatus.DRAFT;
    }

    public void retire() {
        if (status == ReleaseStatus.RETIRED) {
            throw new IllegalStateException("Release is already retired");
        }

        status = ReleaseStatus.RETIRED;
    }

    private void requireStatus(ReleaseStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "Expected release status " + expected
                            + " but found " + status
            );
        }
    }

    @PrePersist
    private void beforeInsert() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void beforeUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Application getApplication() {
        return application;
    }

    public String getVersion() {
        return version;
    }

    public String getGitCommitSha() {
        return gitCommitSha;
    }

    public String getDockerImage() {
        return dockerImage;
    }

    public String getImageDigest() {
        return imageDigest;
    }

    public ReleaseStatus getStatus() {
        return status;
    }

    public Long getLockVersion() {
        return lockVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}