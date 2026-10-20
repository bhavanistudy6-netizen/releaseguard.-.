package com.releaseguard.deployment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeploymentRepository
        extends JpaRepository<Deployment, Long> {

    Optional<Deployment> findByIdAndApplication_Id(
            Long deploymentId,
            Long applicationId);

    Page<Deployment> findByApplication_IdAndEnvironment(
            Long applicationId,
            DeploymentEnvironment environment,
            Pageable pageable);
}