package com.releaseguard.comparison;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/releases/compare")
public class ReleaseComparisonController {

    private final ReleaseComparisonService service;

    public ReleaseComparisonController(ReleaseComparisonService service) {
        this.service = service;
    }

    @GetMapping
    public ReleaseComparisonResponse compare(
            @PathVariable Long applicationId,
            @RequestParam Long fromReleaseId,
            @RequestParam Long toReleaseId) {

        return service.compare(
                applicationId,
                fromReleaseId,
                toReleaseId);
    }
}