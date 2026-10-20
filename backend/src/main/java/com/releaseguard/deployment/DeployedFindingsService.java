package com.releaseguard.deployment;

import com.releaseguard.application.ApplicationRepository;
import com.releaseguard.vulnerability.VulnerabilityFindingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DeployedFindingsService {

    private final ApplicationRepository applicationRepository;
    private final CurrentDeploymentRepository currentRepository;
    private final VulnerabilityFindingService findingService;

    public DeployedFindingsService(
            ApplicationRepository applicationRepository,
            CurrentDeploymentRepository currentRepository,
            VulnerabilityFindingService findingService) {
        this.applicationRepository = applicationRepository;
        this.currentRepository = currentRepository;
        this.findingService = findingService;
    }

    @Transactional(readOnly = true)
    public DeployedFindingsResponse list(
            Long applicationId,
            DeploymentEnvironment environment,
            Long importId,
            int page,
            int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be at least 0 and size must be between 1 and 100");
        }

        if (!applicationRepository.existsById(applicationId)) {
            throw notFound("Application not found");
        }

        var key = new CurrentDeploymentId(
                applicationId,
                environment);

        var current = currentRepository.findById(key)
                .orElseThrow(() -> notFound(
                        "No current deployment recorded for this environment"));

        Deployment deployment = current.getDeployment();

        // This service also checks that the import belongs
        // to the deployed release's SBOM.
        var findings = findingService.list(
                applicationId,
                deployment.getRelease().getId(),
                importId,
                page,
                size);

        return new DeployedFindingsResponse(
                DeploymentResponse.from(deployment),
                importId,
                findings);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message);
    }
}