package com.releaseguard.deployment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CurrentDeploymentRepository
        extends JpaRepository<CurrentDeployment, CurrentDeploymentId> {

    List<CurrentDeployment> findById_ApplicationId(
            Long applicationId);
}