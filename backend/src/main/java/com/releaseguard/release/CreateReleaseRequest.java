package com.releaseguard.release;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateReleaseRequest(

        @NotBlank
        @Size(max = 100)
        String version,

        @NotBlank
        @Pattern(
                regexp = "([0-9a-fA-F]{40}|[0-9a-fA-F]{64})",
                message = "Git commit must be a full 40 or 64 character hexadecimal hash"
        )
        String gitCommitSha,

        @NotBlank
        @Size(max = 500)
        String dockerImage,

        @Pattern(
                regexp = "sha256:[0-9a-fA-F]{64}",
                message = "Image digest must use sha256 followed by 64 hexadecimal characters"
        )
        String imageDigest
) {
}