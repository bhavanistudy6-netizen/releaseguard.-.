package com.releaseguard.approval;

import com.releaseguard.application.ApplicationRepository;
import com.releaseguard.release.Release;
import com.releaseguard.release.ReleaseRepository;
import com.releaseguard.sbom.SbomDocumentRepository;
import com.releaseguard.vulnerability.VulnerabilityFindingRepository;
import com.releaseguard.vulnerability.VulnerabilityImport;
import com.releaseguard.vulnerability.VulnerabilityImportRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@Validated
public class ReleaseDecisionService {

    private final ApplicationRepository applicationRepository;
    private final ReleaseRepository releaseRepository;
    private final SbomDocumentRepository documentRepository;
    private final VulnerabilityImportRepository importRepository;
    private final VulnerabilityFindingRepository findingRepository;
    private final ReleaseDecisionRepository decisionRepository;
    private final ReleaseApprovalPolicy policy;

    public ReleaseDecisionService(
            ApplicationRepository applicationRepository,
            ReleaseRepository releaseRepository,
            SbomDocumentRepository documentRepository,
            VulnerabilityImportRepository importRepository,
            VulnerabilityFindingRepository findingRepository,
            ReleaseDecisionRepository decisionRepository,
            ReleaseApprovalPolicy policy) {
        this.applicationRepository = applicationRepository;
        this.releaseRepository = releaseRepository;
        this.documentRepository = documentRepository;
        this.importRepository = importRepository;
        this.findingRepository = findingRepository;
        this.decisionRepository = decisionRepository;
        this.policy = policy;
    }

    @Transactional
    public ReleaseDecisionResponse decide(
            Long applicationId,
            Long releaseId,
            @Valid ReleaseDecisionRequest request) {

        applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "Application not found"));

        Release release = releaseRepository
                .findByIdAndApplication_Id(releaseId, applicationId)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "Release not found for this application"));

        VulnerabilityImport evidence = null;
        String policyVersion = null;

        if (request.action() == ReleaseDecisionAction.APPROVE) {
            evidence = checkApprovalEvidence(
                    releaseId,
                    request.vulnerabilityImportId());
            policyVersion = ReleaseApprovalPolicy.VERSION;
        } else if (request.vulnerabilityImportId() != null) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Scan evidence is accepted only for APPROVE");
        }

        var fromStatus = release.getStatus();

        try {
            switch (request.action()) {
                case SUBMIT -> release.submitForReview();
                case APPROVE -> release.approve();
                case REJECT -> release.reject();
                case REVOKE -> release.revokeApproval();
                case RETIRE -> release.retire();
            }
        } catch (IllegalStateException exception) {
            throw error(HttpStatus.CONFLICT, exception.getMessage());
        }

        releaseRepository.saveAndFlush(release);

        ReleaseDecision decision = new ReleaseDecision(
                release,
                request.action(),
                fromStatus,
                release.getStatus(),
                request.actor().strip(),
                request.reason().strip(),
                evidence,
                policyVersion);

        return ReleaseDecisionResponse.from(
                decisionRepository.saveAndFlush(decision));
    }

    private VulnerabilityImport checkApprovalEvidence(
            Long releaseId,
            Long importId) {

        if (importId == null) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Approval requires a vulnerability import ID");
        }

        var document = documentRepository.findByRelease_Id(releaseId)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "Release has no SBOM"));

        var scan = importRepository
                .findByIdAndSbomDocument_Id(importId, document.getId())
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "Scan import not found for this release"));

        var severities = findingRepository
                .findDistinctSeveritiesByImportId(scan.getId());

        ApprovalPolicyResult result = policy.evaluate(
                scan.getScannedAt(),
                severities,
                Instant.now());

        if (!result.allowed()) {
            throw error(
                    HttpStatus.CONFLICT,
                    String.join("; ", result.blockers()));
        }

        return scan;
    }

    private ResponseStatusException error(
            HttpStatus status,
            String message) {
        return new ResponseStatusException(status, message);
    }
}