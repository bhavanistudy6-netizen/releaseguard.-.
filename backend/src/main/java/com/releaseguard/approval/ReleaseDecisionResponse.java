package com.releaseguard.approval;

import com.releaseguard.release.ReleaseStatus;

import java.time.Instant;

public record ReleaseDecisionResponse(
        Long id,
        Long releaseId,
        ReleaseDecisionAction action,
        ReleaseStatus fromStatus,
        ReleaseStatus toStatus,
        String actor,
        String reason,
        Long vulnerabilityImportId,
        String policyVersion,
        Instant decidedAt,
        Long actorUserId,
        boolean legacyCallerSupplied) {

    public ReleaseDecisionResponse(
            Long id,
            Long releaseId,
            ReleaseDecisionAction action,
            ReleaseStatus fromStatus,
            ReleaseStatus toStatus,
            String actor,
            String reason,
            Long vulnerabilityImportId,
            String policyVersion,
            Instant decidedAt) {
        this(
                id,
                releaseId,
                action,
                fromStatus,
                toStatus,
                actor,
                reason,
                vulnerabilityImportId,
                policyVersion,
                decidedAt,
                null,
                true
        );
    }

    public static ReleaseDecisionResponse from(ReleaseDecision decision) {
        var scan = decision.getVulnerabilityImport();
        Long actorUserId = decision.getActorUserId();
        boolean legacy = (actorUserId == null);

        return new ReleaseDecisionResponse(
                decision.getId(),
                decision.getRelease().getId(),
                decision.getAction(),
                decision.getFromStatus(),
                decision.getToStatus(),
                decision.getActor(),
                decision.getReason(),
                scan == null ? null : scan.getId(),
                decision.getPolicyVersion(),
                decision.getDecidedAt(),
                actorUserId,
                legacy);
    }
}
