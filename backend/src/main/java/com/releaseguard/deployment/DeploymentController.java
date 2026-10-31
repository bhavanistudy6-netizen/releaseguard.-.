package com.releaseguard.deployment;

import com.releaseguard.security.ReleaseGuardUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
            @Valid @RequestBody RecordDeploymentRequest request,
            Authentication authentication) {

        String deployedBy;
        Long deployedByUserId = null;

        if (authentication != null && authentication.getPrincipal() instanceof ReleaseGuardUserDetails userDetails) {
            deployedBy = userDetails.getDisplayName() != null && !userDetails.getDisplayName().isBlank()
                    ? userDetails.getDisplayName() + " (" + userDetails.getUsername() + ")"
                    : userDetails.getUsername();
            deployedByUserId = userDetails.getId();
        } else if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            deployedBy = authentication.getName();
        } else if (request.deployedBy() != null && !request.deployedBy().isBlank()) {
            deployedBy = request.deployedBy().strip();
        } else {
            deployedBy = "authenticated-deployer";
        }

        DeploymentResponse response = service.record(applicationId, request, deployedBy, deployedByUserId);

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
