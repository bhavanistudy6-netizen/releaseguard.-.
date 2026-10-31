package com.releaseguard.approval;

import com.releaseguard.release.Release;
import com.releaseguard.release.ReleaseStatus;
import com.releaseguard.sbom.SbomDocument;
import com.releaseguard.vulnerability.VulnerabilityImport;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "release_decisions")
public class ReleaseDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "release_id", nullable = false)
    private Release release;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReleaseDecisionAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 30)
    private ReleaseStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private ReleaseStatus toStatus;

    @Column(nullable = false, length = 150)
    private String actor;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "decided_at", nullable = false, updatable = false)
    private Instant decidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sbom_document_id")
    private SbomDocument sbomDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vulnerability_import_id")
    private VulnerabilityImport vulnerabilityImport;

    @Column(name = "policy_version", length = 50)
    private String policyVersion;

    protected ReleaseDecision() {
    }

    public ReleaseDecision(
            Release release,
            ReleaseDecisionAction action,
            ReleaseStatus fromStatus,
            ReleaseStatus toStatus,
            String actor,
            String reason,
            VulnerabilityImport vulnerabilityImport,
            String policyVersion) {
        this(release, action, fromStatus, toStatus, actor, reason, vulnerabilityImport, policyVersion, null);
    }

    public ReleaseDecision(
            Release release,
            ReleaseDecisionAction action,
            ReleaseStatus fromStatus,
            ReleaseStatus toStatus,
            String actor,
            String reason,
            VulnerabilityImport vulnerabilityImport,
            String policyVersion,
            Long actorUserId) {
        this.release = release;
        this.action = action;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actor = actor;
        this.reason = reason;
        this.vulnerabilityImport = vulnerabilityImport;
        this.sbomDocument = vulnerabilityImport == null
                ? null
                : vulnerabilityImport.getSbomDocument();
        this.policyVersion = policyVersion;
        this.actorUserId = actorUserId;
    }

    @PrePersist
    void onCreate() {
        decidedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Release getRelease() {
        return release;
    }

    public ReleaseDecisionAction getAction() {
        return action;
    }

    public ReleaseStatus getFromStatus() {
        return fromStatus;
    }

    public ReleaseStatus getToStatus() {
        return toStatus;
    }

    public String getActor() {
        return actor;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public String getReason() {
        return reason;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public SbomDocument getSbomDocument() {
        return sbomDocument;
    }

    public VulnerabilityImport getVulnerabilityImport() {
        return vulnerabilityImport;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }
}
