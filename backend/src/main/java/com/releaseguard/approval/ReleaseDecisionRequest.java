package com.releaseguard.approval;

import jakarta.validation.constraints.*;

public record ReleaseDecisionRequest(

        @NotNull ReleaseDecisionAction action,

        @NotBlank @Size(max = 150) String actor,

        @NotBlank @Size(max = 5000) String reason,

        @Positive Long vulnerabilityImportId) {
}