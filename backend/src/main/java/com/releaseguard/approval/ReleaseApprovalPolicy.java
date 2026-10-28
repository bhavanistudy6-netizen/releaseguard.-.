package com.releaseguard.approval;

import com.releaseguard.vulnerability.VulnerabilitySeverity;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Component
public class ReleaseApprovalPolicy {

    public static final String VERSION = "release-policy-v1";
    private static final Duration MAX_SCAN_AGE = Duration.ofDays(7);

    public ApprovalPolicyResult evaluate(
            Instant scannedAt,
            Collection<VulnerabilitySeverity> severities,
            Instant now) {

        Objects.requireNonNull(now, "Current time is required");

        List<String> blockers = new ArrayList<>();

        if (scannedAt == null) {
            blockers.add("Scan timestamp is missing");
        } else if (scannedAt.isAfter(now)) {
            blockers.add("Scan timestamp is in the future");
        } else if (scannedAt.isBefore(now.minus(MAX_SCAN_AGE))) {
            blockers.add("Scan is older than 7 days");
        }

        if (severities == null) {
            blockers.add("Scan finding information is unavailable");
        } else {
            if (severities.stream().anyMatch(Objects::isNull)) {
                blockers.add("A finding has no severity");
            }

            for (VulnerabilitySeverity severity : List.of(
                    VulnerabilitySeverity.CRITICAL,
                    VulnerabilitySeverity.HIGH,
                    VulnerabilitySeverity.UNKNOWN)) {
                if (severities.contains(severity)) {
                    blockers.add("Scan contains " + severity + " findings");
                }
            }
        }

        return new ApprovalPolicyResult(VERSION, blockers);
    }
}