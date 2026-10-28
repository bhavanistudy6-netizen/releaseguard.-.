package com.releaseguard.deployment;

import com.releaseguard.application.ApplicationRepository;
import com.releaseguard.approval.ProductionDeploymentPolicy;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
public class RollbackService {

    private final ApplicationRepository applicationRepository;
    private final DeploymentRepository deploymentRepository;
    private final CurrentDeploymentRepository currentRepository;
    private final ProductionDeploymentPolicy productionPolicy;

    public RollbackService(
            ApplicationRepository applicationRepository,
            DeploymentRepository deploymentRepository,
            CurrentDeploymentRepository currentRepository,
            ProductionDeploymentPolicy productionPolicy) {

        this.applicationRepository = applicationRepository;
        this.deploymentRepository = deploymentRepository;
        this.currentRepository = currentRepository;
        this.productionPolicy = productionPolicy;
    }

    @Transactional
    public RollbackResponse record(
            Long applicationId,
            @Valid RecordRollbackRequest request) {

        applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "Application not found"));

        CurrentDeploymentId key = new CurrentDeploymentId(
                applicationId,
                request.environment());

        var current = currentRepository.findById(key)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "No current deployment recorded for this environment"));

        Deployment from = current.getDeployment();

        if (!from.getId().equals(request.expectedCurrentDeploymentId())) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Current deployment has changed; refresh before recording a rollback");
        }

        Deployment target = deploymentRepository
                .findByIdAndApplication_Id(
                        request.targetDeploymentId(),
                        applicationId)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "Rollback target not found for this application"));

        if (target.getEnvironment() != request.environment()) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Rollback target belongs to another environment");
        }

        if (target.getId() >= from.getId()) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Rollback target must precede the current deployment");
        }

        if (request.environment() == DeploymentEnvironment.PRODUCTION) {
            productionPolicy.check(target.getRelease());
        }

        Deployment rollback = Deployment.rollback(
                from,
                target,
                request.deployedBy().strip(),
                request.deployedAt(),
                request.reason().strip());

        Deployment saved = deploymentRepository.saveAndFlush(rollback);

        current.changeDeployment(saved);
        currentRepository.saveAndFlush(current);

        return RollbackResponse.from(saved);
    }

    private ResponseStatusException error(
            HttpStatus status,
            String message) {

        return new ResponseStatusException(status, message);
    }
}