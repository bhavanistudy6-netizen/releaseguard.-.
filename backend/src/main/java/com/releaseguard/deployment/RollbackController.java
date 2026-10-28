package com.releaseguard.deployment;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @Valid @RequestBody RecordRollbackRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.record(applicationId, request));
    }
}