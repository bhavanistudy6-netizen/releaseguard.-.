package com.releaseguard.deployment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/deployments")
public class DeploymentController {

    private final DeploymentService service;

    public DeploymentController(DeploymentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DeploymentResponse> record(
            @PathVariable Long applicationId,
            @Valid @RequestBody RecordDeploymentRequest request) {

        DeploymentResponse response = service.record(applicationId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/current")
    public DeploymentResponse getCurrent(
            @PathVariable Long applicationId,
            @RequestParam DeploymentEnvironment environment) {

        return service.getCurrent(applicationId, environment);
    }

    @GetMapping
    public DeploymentPageResponse history(
            @PathVariable Long applicationId,
            @RequestParam DeploymentEnvironment environment,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return service.history(applicationId, environment, page, size);
    }
}