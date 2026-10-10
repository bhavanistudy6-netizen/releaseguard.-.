package com.releaseguard.application;

import java.time.Instant;

public record ApplicationResponse(
        Long id,
        String name,
        String description,
        String repositoryUrl,
        String ownerTeam,
        Instant createdAt,
        Instant updatedAt
) {

    public static ApplicationResponse from(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getName(),
                application.getDescription(),
                application.getRepositoryUrl(),
                application.getOwnerTeam(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}