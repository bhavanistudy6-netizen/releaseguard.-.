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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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
        String actor = (request.actor() != null && !request.actor().isBlank())
                ? request.actor().strip()
                : "anonymous";
        return decide(applicationId, releaseId, request, actor, null);
    }

    @Transactional
    public ReleaseDecisionResponse decide(
            Long applicationId,
            Long releaseId,
            @Valid ReleaseDecisionRequest request,
            String actorName,
            Long actorUserId) {

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

        String resolvedActor = (actorName != null && !actorName.isBlank())
                ? actorName.strip()
                : (request.actor() != null && !request.actor().isBlank() ? request.actor().strip() : "anonymous");

        ReleaseDecision decision = new ReleaseDecision(
                release,
                request.action(),
                fromStatus,
                release.getStatus(),
                resolvedActor,
                request.reason().strip(),
                evidence,
                policyVersion,
                actorUserId);

        return ReleaseDecisionResponse.from(
                decisionRepository.saveAndFlush(decision));
    }

    @Transactional(readOnly = true)
    public ReleaseDecisionPageResponse history(
            Long applicationId,
            Long releaseId,
            int page,
            int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100");
        }

        releaseRepository
                .findByIdAndApplication_Id(releaseId, applicationId)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "Release not found for this application"));

        var decisions = decisionRepository.findByRelease_Id(
                releaseId,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.DESC, "id")));

        var content = decisions.getContent()
                .stream()
                .map(ReleaseDecisionResponse::from)
                .toList();

        return new ReleaseDecisionPageResponse(
                content,
                decisions.getNumber(),
                decisions.getSize(),
                decisions.getTotalElements(),
                decisions.getTotalPages());
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
