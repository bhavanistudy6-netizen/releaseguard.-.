package com.releaseguard.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApplicationRequest(

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 5000)
        String description,

        @Size(max = 500)
        String repositoryUrl,

        @NotBlank
        @Size(max = 150)
        String ownerTeam
) {
}