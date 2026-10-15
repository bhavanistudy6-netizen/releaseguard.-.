package com.releaseguard.sbom;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SbomDocumentRepository
        extends JpaRepository<SbomDocument, Long> {

    boolean existsByRelease_Id(Long releaseId);

    Optional<SbomDocument> findByRelease_Id(Long releaseId);
}