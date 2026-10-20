package com.releaseguard.deployment;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/deployments/current/findings")
public class DeployedFindingsController {

    private final DeployedFindingsService service;

    public DeployedFindingsController(DeployedFindingsService service) {
        this.service = service;
    }

    @GetMapping
    public DeployedFindingsResponse list(
            @PathVariable Long applicationId,
            @RequestParam DeploymentEnvironment environment,
            @RequestParam Long importId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return service.list(
                applicationId,
                environment,
                importId,
                page,
                size);
    }
}