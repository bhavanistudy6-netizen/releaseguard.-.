package com.releaseguard.deployment;

import com.releaseguard.vulnerability.VulnerabilityFindingPageResponse;

public record DeployedFindingsResponse(
        DeploymentResponse deployment,
        Long importId,
        VulnerabilityFindingPageResponse findings) {
}