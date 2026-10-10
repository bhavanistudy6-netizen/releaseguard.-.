package com.releaseguard.application;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> create(
            @Valid @RequestBody CreateApplicationRequest request) {

        ApplicationResponse response = service.create(request);

        URI location = URI.create(
                "/api/v1/applications/" + response.id()
        );

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ApplicationResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public ApplicationPageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return service.list(page, size);
    }
}