package com.releaseguard.approval;

import com.releaseguard.release.Release;
import com.releaseguard.release.ReleaseStatus;
import com.releaseguard.vulnerability.VulnerabilityFindingRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Component
public class ProductionDeploymentPolicy {

    private final ReleaseDecisionRepository decisionRepository;
    private final VulnerabilityFindingRepository findingRepository;
    private final ReleaseApprovalPolicy approvalPolicy;

    public ProductionDeploymentPolicy(
            ReleaseDecisionRepository decisionRepository,
            VulnerabilityFindingRepository findingRepository,
            ReleaseApprovalPolicy approvalPolicy) {

        this.decisionRepository = decisionRepository;
        this.findingRepository = findingRepository;
        this.approvalPolicy = approvalPolicy;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void check(Release release) {
        if (release.getStatus() != ReleaseStatus.APPROVED) {
            throw blocked(
                    "Production deployment requires an approved release");
        }

        ReleaseDecision decision = decisionRepository
                .findFirstByRelease_IdOrderByIdDesc(release.getId())
                .orElseThrow(() -> blocked(
                        "Release has no approval decision"));

        if (decision.getAction() != ReleaseDecisionAction.APPROVE
                || decision.getToStatus() != ReleaseStatus.APPROVED) {

            throw blocked(
                    "The latest release decision must be an approval");
        }

        if (!ReleaseApprovalPolicy.VERSION.equals(
                decision.getPolicyVersion())) {

            throw blocked(
                    "Release must be approved under the current policy");
        }

        var scan = decision.getVulnerabilityImport();
        var document = decision.getSbomDocument();

        if (scan == null || document == null) {
            throw blocked(
                    "Approval is missing its scan evidence");
        }

        if (!release.getId().equals(document.getRelease().getId())
                || !document.getId().equals(
                        scan.getSbomDocument().getId())) {

            throw blocked(
                    "Approval evidence does not belong to this release");
        }

        var severities = findingRepository
                .findDistinctSeveritiesByImportId(scan.getId());

        ApprovalPolicyResult result = approvalPolicy.evaluate(
                scan.getScannedAt(),
                severities,
                Instant.now());

        if (!result.allowed()) {
            throw blocked(
                    "Approval evidence no longer passes policy: "
                            + String.join("; ", result.blockers()));
        }
    }

    private ResponseStatusException blocked(String message) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message);
    }
}