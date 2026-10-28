package com.releaseguard.approval;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @Valid @RequestBody ReleaseDecisionRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.decide(applicationId, releaseId, request));
    }
}