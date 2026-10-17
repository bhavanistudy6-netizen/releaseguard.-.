package com.releaseguard.sbom;

import java.time.Instant;

public record SbomUploadResponse(
        Long id,
        Long releaseId,
        String originalFilename,
        String bomFormat,
        String specVersion,
        String documentSha256,
        int componentCount,
        Instant uploadedAt) {
}