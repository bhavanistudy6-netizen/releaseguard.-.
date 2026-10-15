package com.releaseguard.release;

import com.releaseguard.application.Application;
import com.releaseguard.application.ApplicationRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final ApplicationRepository applicationRepository;

    public ReleaseService(
            ReleaseRepository releaseRepository,
            ApplicationRepository applicationRepository) {
        this.releaseRepository = releaseRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public ReleaseResponse create(
            Long applicationId,
            CreateReleaseRequest request) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application not found"
                ));

        String version = request.version().strip();

        if (releaseRepository.existsByApplication_IdAndVersion(
                applicationId, version)) {
            throw duplicateVersion();
        }

        Release release = new Release(
                application,
                version,
                request.gitCommitSha(),
                request.dockerImage().strip(),
                request.imageDigest()
        );

        try {
            Release saved = releaseRepository.saveAndFlush(release);
            return ReleaseResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            Throwable cause = exception;

            while (cause != null) {
                if (cause instanceof ConstraintViolationException violation
                        && "uq_releases_application_version"
                                .equals(violation.getConstraintName())) {
                    throw duplicateVersion();
                }

                cause = cause.getCause();
            }

            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public ReleaseResponse getById(Long applicationId, Long releaseId) {
        Release release = releaseRepository
                .findByIdAndApplication_Id(releaseId, applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Release not found for this application"
                ));

        return ReleaseResponse.from(release);
    }

    private ResponseStatusException duplicateVersion() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "This version already exists for this application"
        );
    }

    @Transactional(readOnly = true)
    public ReleasePageResponse list(
        Long applicationId,
        int page,
        int size) {
            if (page < 0 || size < 1 || size > 100) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be at least 0 and size must be between 1 and 100"
                );
            }

            if (!applicationRepository.existsById(applicationId)) {
                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Application not found"
                );
            }

            PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")
            );

            Page<Release> releases = releaseRepository.findByApplication_Id(
                applicationId,
                pageable
            );

            return new ReleasePageResponse(
                releases.getContent()
                    .stream()
                    .map(ReleaseResponse::from)
                    .toList(),
                releases.getNumber(),
                releases.getSize(),
                releases.getTotalElements(),
                releases.getTotalPages()
            );
    }
}