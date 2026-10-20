package com.releaseguard.deployment;

import com.releaseguard.application.ApplicationRepository;
import com.releaseguard.release.ReleaseRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
public class DeploymentService {

    private final ApplicationRepository applicationRepository;
    private final ReleaseRepository releaseRepository;
    private final DeploymentRepository deploymentRepository;
    private final CurrentDeploymentRepository currentRepository;

    public DeploymentService(
            ApplicationRepository applicationRepository,
            ReleaseRepository releaseRepository,
            DeploymentRepository deploymentRepository,
            CurrentDeploymentRepository currentRepository) {
        this.applicationRepository = applicationRepository;
        this.releaseRepository = releaseRepository;
        this.deploymentRepository = deploymentRepository;
        this.currentRepository = currentRepository;
    }

    @Transactional
    public DeploymentResponse record(
            Long applicationId,
            @Valid RecordDeploymentRequest request) {

        // Lock an existing row before reading or creating the pointer.
        applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application not found"));

        var release = releaseRepository
                .findByIdAndApplication_Id(
                        request.releaseId(),
                        applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Release not found for this application"));

        Deployment deployment = new Deployment(
                release,
                request.environment(),
                request.deployedBy().strip(),
                request.deployedAt(),
                request.notes());

        Deployment saved = deploymentRepository.saveAndFlush(deployment);

        CurrentDeploymentId currentId = new CurrentDeploymentId(
                applicationId,
                request.environment());

        CurrentDeployment current = currentRepository
                .findById(currentId)
                .orElse(null);

        if (current == null) {
            current = new CurrentDeployment(saved);
        } else {
            current.changeDeployment(saved);
        }

        currentRepository.saveAndFlush(current);

        return DeploymentResponse.from(saved);
    }
}