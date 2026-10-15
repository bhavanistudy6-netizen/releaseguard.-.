package com.releaseguard.release;

import java.time.Instant;

public record ReleaseResponse(
        Long id,
        Long applicationId,
        String version,
        String gitCommitSha,
        String dockerImage,
        String imageDigest,
        ReleaseStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static ReleaseResponse from(Release release) {
        return new ReleaseResponse(
                release.getId(),
                release.getApplication().getId(),
                release.getVersion(),
                release.getGitCommitSha(),
                release.getDockerImage(),
                release.getImageDigest(),
                release.getStatus(),
                release.getCreatedAt(),
                release.getUpdatedAt()
        );
    }
}