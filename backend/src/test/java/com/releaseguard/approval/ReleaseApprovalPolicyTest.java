package com.releaseguard.approval;

import com.releaseguard.vulnerability.VulnerabilitySeverity;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseApprovalPolicyTest {

    private final ReleaseApprovalPolicy policy =
            new ReleaseApprovalPolicy();

    private final Instant now =
            Instant.parse("2026-10-28T12:00:00Z");

    @Test
    void allowsFreshScanWithZeroFindings() {
        var result = policy.evaluate(now, List.of(), now);

        assertTrue(result.allowed());
        assertEquals("release-policy-v1", result.policyVersion());
    }

    @Test
    void allowsMediumLowAndInformationalFindings() {
        var result = policy.evaluate(
                now,
                List.of(
                        VulnerabilitySeverity.MEDIUM,
                        VulnerabilitySeverity.LOW,
                        VulnerabilitySeverity.INFORMATIONAL
                ),
                now
        );

        assertTrue(result.allowed());
    }

    @Test
    void blocksEachDisallowedSeverity() {
        for (var severity : List.of(
                VulnerabilitySeverity.CRITICAL,
                VulnerabilitySeverity.HIGH,
                VulnerabilitySeverity.UNKNOWN
        )) {
            var result = policy.evaluate(
                    now,
                    List.of(severity),
                    now
            );

            assertFalse(result.allowed());
            assertTrue(result.blockers().contains(
                    "Scan contains " + severity + " findings"
            ));
        }
    }

    @Test
    void blocksScanOlderThanSevenDays() {
        var result = policy.evaluate(
                now.minus(Duration.ofDays(7)).minusSeconds(1),
                List.of(),
                now
        );

        assertFalse(result.allowed());
        assertTrue(result.blockers().contains("Scan is older than 7 days"));
    }

    @Test
    void allowsScanExactlySevenDaysOld() {
        var result = policy.evaluate(
                now.minus(Duration.ofDays(7)),
                List.of(),
                now
        );

        assertTrue(result.allowed());
    }

    @Test
    void blocksFutureScanTimestamp() {
        var result = policy.evaluate(
                now.plusSeconds(1),
                List.of(),
                now
        );

        assertFalse(result.allowed());
        assertTrue(result.blockers().contains(
                "Scan timestamp is in the future"
        ));
    }

    @Test
    void blocksMissingScanTimestamp() {
        var result = policy.evaluate(null, List.of(), now);

        assertFalse(result.allowed());
    }

    @Test
    void blocksUnavailableFindingInformation() {
        var result = policy.evaluate(now, null, now);

        assertFalse(result.allowed());
    }

    @Test
    void blocksFindingWithoutSeverity() {
        var result = policy.evaluate(
                now,
                Arrays.asList(VulnerabilitySeverity.LOW, null),
                now
        );

        assertFalse(result.allowed());
        assertTrue(result.blockers().contains("A finding has no severity"));
    }
}