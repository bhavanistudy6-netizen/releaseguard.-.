package com.releaseguard.release;

import java.net.URI;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/releases")
public class ReleaseController {

    private final ReleaseService releaseService;

    public ReleaseController(ReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    @PostMapping
    public ResponseEntity<ReleaseResponse> create(
            @PathVariable Long applicationId,
            @Valid @RequestBody CreateReleaseRequest request) {

        ReleaseResponse response = releaseService.create(
                applicationId, request
        );

        URI location = URI.create(
                "/api/v1/applications/"
                        + applicationId
                        + "/releases/"
                        + response.id()
        );

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{releaseId}")
    public ReleaseResponse getById(
            @PathVariable Long applicationId,
            @PathVariable Long releaseId) {

        return releaseService.getById(applicationId, releaseId);
    }

    @GetMapping
    public ReleasePageResponse list(
            @PathVariable Long applicationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return releaseService.list(applicationId, page, size);
    }
}