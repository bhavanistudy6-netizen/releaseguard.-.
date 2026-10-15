package com.releaseguard.sbom;

import com.releaseguard.release.Release;
import com.releaseguard.release.ReleaseRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
public class SbomService {

    private final ReleaseRepository releaseRepository;
    private final SbomDocumentRepository documentRepository;
    private final SbomComponentRepository componentRepository;
    private final CycloneDxParser parser;

    public SbomService(
            ReleaseRepository releaseRepository,
            SbomDocumentRepository documentRepository,
            SbomComponentRepository componentRepository,
            CycloneDxParser parser) {
        this.releaseRepository = releaseRepository;
        this.documentRepository = documentRepository;
        this.componentRepository = componentRepository;
        this.parser = parser;
    }

    @Transactional
    public SbomUploadResponse upload(
            Long applicationId,
            Long releaseId,
            String originalFilename,
            byte[] content) {

        Release release = releaseRepository
                .findByIdAndApplication_Id(releaseId, applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Release not found for this application"));

        if (documentRepository.existsByRelease_Id(releaseId)) {
            throw duplicateSbom();
        }

        String filename = cleanFilename(originalFilename);
        ParsedSbom parsed = parser.parse(content);

        SbomDocument document = new SbomDocument(
                release,
                filename,
                parsed.bomFormat(),
                parsed.specVersion(),
                sha256(content));

        SbomDocument saved;

        try {
            saved = documentRepository.saveAndFlush(document);
        } catch (DataIntegrityViolationException exception) {
            Throwable cause = exception;

            while (cause != null) {
                if (cause instanceof ConstraintViolationException violation
                        && "uq_sbom_documents_release"
                                .equals(violation.getConstraintName())) {
                    throw duplicateSbom();
                }

                cause = cause.getCause();
            }

            throw exception;
        }

        List<SbomComponent> components = parsed.components()
                .stream()
                .map(component -> new SbomComponent(
                        saved,
                        component.bomRef(),
                        component.componentType(),
                        component.groupName(),
                        component.name(),
                        component.version(),
                        component.purl()))
                .toList();

        componentRepository.saveAllAndFlush(components);

        return new SbomUploadResponse(
                saved.getId(),
                releaseId,
                saved.getOriginalFilename(),
                saved.getBomFormat(),
                saved.getSpecVersion(),
                saved.getDocumentSha256(),
                components.size(),
                saved.getUploadedAt());
    }

    private String cleanFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "SBOM filename is required");
        }

        // Keep only the filename, removing any client-supplied path.
        String normalized = originalFilename.replace('\\', '/');
        String filename = normalized
                .substring(normalized.lastIndexOf('/') + 1)
                .strip();

        if (filename.isBlank()
                || filename.length() > 255
                || filename.chars().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "SBOM filename must contain 1 to 255 characters "
                            + "without control characters");
        }

        return filename;
    }

    private String sha256(byte[] content) {
        try {
            byte[] digest = MessageDigest
                    .getInstance("SHA-256")
                    .digest(content);

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception);
        }
    }

    private ResponseStatusException duplicateSbom() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "An SBOM already exists for this release");
    }
}