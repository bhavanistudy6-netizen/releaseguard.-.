package com.releaseguard.sbom;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SbomComponentRepository
        extends JpaRepository<SbomComponent, Long> {

    Page<SbomComponent> findBySbomDocument_Id(
            Long sbomDocumentId,
            Pageable pageable);
}