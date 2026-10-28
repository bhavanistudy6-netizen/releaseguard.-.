package com.releaseguard.approval;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReleaseDecisionRepository
        extends JpaRepository<ReleaseDecision, Long> {

    Page<ReleaseDecision> findByRelease_Id(
            Long releaseId,
            Pageable pageable);

    Optional<ReleaseDecision> findFirstByRelease_IdOrderByIdDesc(
            Long releaseId);
}