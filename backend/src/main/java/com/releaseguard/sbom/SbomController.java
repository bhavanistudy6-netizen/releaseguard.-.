package com.releaseguard.sbom;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/releases/{releaseId}/sbom")
public class SbomController {

    private static final int MAX_BYTES = 10 * 1024 * 1024;

    private final SbomService sbomService;

    public SbomController(SbomService sbomService) {
        this.sbomService = sbomService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SbomUploadResponse> upload(
            @PathVariable Long applicationId,
            @PathVariable Long releaseId,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "SBOM file must not be empty");
        }

        if (file.getSize() > MAX_BYTES) {
            throw tooLarge();
        }

        byte[] content;

        try (InputStream input = file.getInputStream()) {
            content = input.readNBytes(MAX_BYTES + 1);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to read the uploaded SBOM",
                    exception);
        }

        if (content.length > MAX_BYTES) {
            throw tooLarge();
        }

        SbomUploadResponse response = sbomService.upload(
                applicationId,
                releaseId,
                file.getOriginalFilename(),
                content);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    private ResponseStatusException tooLarge() {
        return new ResponseStatusException(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "SBOM file must not exceed 10 MiB");
    }

    @GetMapping
    public SbomUploadResponse getSbom(
            @PathVariable Long applicationId,
            @PathVariable Long releaseId) {

        return sbomService.getSbom(applicationId, releaseId);
    }

    @GetMapping("/components")
    public SbomComponentPageResponse listComponents(
            @PathVariable Long applicationId,
            @PathVariable Long releaseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return sbomService.listComponents(
                applicationId,
                releaseId,
                page,
                size);
    }
}
