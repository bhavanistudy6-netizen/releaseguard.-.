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
        Instant decidedAt) {
    public static ReleaseDecisionResponse from(ReleaseDecision decision) {
        var scan = decision.getVulnerabilityImport();

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
                decision.getDecidedAt());
    }
}