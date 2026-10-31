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

        RollbackResponse response;
        if (authentication != null && authentication.getPrincipal() instanceof ReleaseGuardUserDetails userDetails) {
            String deployedBy = userDetails.getDisplayName() != null && !userDetails.getDisplayName().isBlank()
                    ? userDetails.getDisplayName() + " (" + userDetails.getUsername() + ")"
                    : userDetails.getUsername();
            Long deployedByUserId = userDetails.getId();
            response = service.record(applicationId, request, deployedBy, deployedByUserId);
        } else if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            response = service.record(applicationId, request, authentication.getName(), null);
        } else {
            response = service.record(applicationId, request);
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
