package com.releaseguard.deployment;

import com.releaseguard.application.ApplicationRepository;
import com.releaseguard.release.ReleaseRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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

    @Transactional(readOnly = true)
    public DeploymentResponse getCurrent(
            Long applicationId,
            DeploymentEnvironment environment) {

        checkApplication(applicationId);

        CurrentDeploymentId id = new CurrentDeploymentId(
                applicationId,
                environment);

        CurrentDeployment current = currentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No current deployment recorded for this environment"));

        return DeploymentResponse.from(current.getDeployment());
    }

    @Transactional(readOnly = true)
    public DeploymentPageResponse history(
            Long applicationId,
            DeploymentEnvironment environment,
            int page,
            int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be at least 0 and size must be between 1 and 100");
        }

        checkApplication(applicationId);

        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id"));

        var deployments = deploymentRepository.findByApplication_IdAndEnvironment(
                applicationId,
                environment,
                pageable);

        return new DeploymentPageResponse(
                deployments.getContent()
                        .stream()
                        .map(DeploymentResponse::from)
                        .toList(),
                deployments.getNumber(),
                deployments.getSize(),
                deployments.getTotalElements(),
                deployments.getTotalPages());
    }

    private void checkApplication(Long applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Application not found");
        }
    }
}