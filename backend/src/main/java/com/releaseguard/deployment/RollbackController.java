package com.releaseguard.deployment;

import com.releaseguard.security.ReleaseGuardUserDetails;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/deployments/rollbacks")
public class RollbackController {

    private final RollbackService service;

    public RollbackController(RollbackService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RollbackResponse> record(
            @PathVariable Long applicationId,
            @Valid @RequestBody RecordRollbackRequest request,
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

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.record(applicationId, request, deployedBy, deployedByUserId));
    }
}
