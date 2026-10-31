package com.releaseguard.approval;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReleaseDecisionRequest(

        @NotNull ReleaseDecisionAction action,

        @Size(max = 150) String actor,

        @NotBlank @Size(max = 5000) String reason,

        @Positive Long vulnerabilityImportId) {
}
