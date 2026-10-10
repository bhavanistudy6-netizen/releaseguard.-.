package com.releaseguard.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class ApplicationService {

    private final ApplicationRepository repository;

    public ApplicationService(ApplicationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ApplicationResponse create(CreateApplicationRequest request) {
        Application application = new Application(
                request.name().strip(),
                request.description(),
                request.repositoryUrl(),
                request.ownerTeam().strip()
        );

        Application saved = repository.save(application);

        return ApplicationResponse.from(saved);

    }

    @Transactional(readOnly = true)
    public ApplicationResponse getById(Long id) {
        Application application = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application not found"
                ));

    return ApplicationResponse.from(application);
    }
    
    @Transactional(readOnly = true)
    public ApplicationPageResponse list(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Page must be non-negative and size must be between 1 and 100"
            );
        }

        PageRequest pageable = PageRequest.of(
            page,
            size,
            Sort.by("id").ascending()
        );

        Page<Application> result = repository.findAll(pageable);

        return new ApplicationPageResponse(
            result.getContent().stream()
                    .map(ApplicationResponse::from)
                    .toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }
}