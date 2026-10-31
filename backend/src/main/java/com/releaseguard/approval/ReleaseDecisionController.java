package com.releaseguard.approval;

import com.releaseguard.security.ReleaseGuardUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/releases/{releaseId}/decisions")
public class ReleaseDecisionController {

    private final ReleaseDecisionService service;

    public ReleaseDecisionController(ReleaseDecisionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ReleaseDecisionResponse> decide(
            @PathVariable Long applicationId,
            @PathVariable Long releaseId,
            @Valid @RequestBody ReleaseDecisionRequest request,
            Authentication authentication) {

        String actorName;
        Long actorUserId = null;

        if (authentication != null && authentication.getPrincipal() instanceof ReleaseGuardUserDetails userDetails) {
            actorName = userDetails.getDisplayName() != null && !userDetails.getDisplayName().isBlank()
                    ? userDetails.getDisplayName() + " (" + userDetails.getUsername() + ")"
                    : userDetails.getUsername();
            actorUserId = userDetails.getId();
        } else if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            actorName = authentication.getName();
        } else if (request.actor() != null && !request.actor().isBlank()) {
            actorName = request.actor().strip();
        } else {
            actorName = "authenticated-user";
        }

        // Action-level authorization
        switch (request.action()) {
            case SUBMIT -> {
                if (!hasAnyRole(authentication, "ROLE_DEVELOPER", "ROLE_ADMIN")) {
                    throw new AccessDeniedException("SUBMIT action requires DEVELOPER or ADMIN role");
                }
            }
            case APPROVE, REJECT, REVOKE, RETIRE -> {
                if (!hasAnyRole(authentication, "ROLE_REVIEWER", "ROLE_ADMIN")) {
                    throw new AccessDeniedException(request.action() + " action requires REVIEWER or ADMIN role");
                }
            }
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.decide(applicationId, releaseId, request, actorName, actorUserId));
    }

    @GetMapping
    public ReleaseDecisionPageResponse history(
            @PathVariable Long applicationId,
            @PathVariable Long releaseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return service.history(applicationId, releaseId, page, size);
    }

    private boolean hasAnyRole(Authentication authentication, String... roles) {
        if (authentication == null) return false;
        for (var authority : authentication.getAuthorities()) {
            for (String role : roles) {
                if (authority.getAuthority().equals(role)) {
                    return true;
                }
            }
        }
        return false;
    }
}
