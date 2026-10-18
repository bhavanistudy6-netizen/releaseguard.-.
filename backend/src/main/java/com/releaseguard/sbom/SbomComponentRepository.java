package com.releaseguard.sbom;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SbomComponentRepository
                extends JpaRepository<SbomComponent, Long> {

        Page<SbomComponent> findBySbomDocument_Id(
                        Long sbomDocumentId,
                        Pageable pageable);

        List<SbomComponent> findAllBySbomDocument_IdOrderByIdAsc(
                        Long sbomDocumentId);
}