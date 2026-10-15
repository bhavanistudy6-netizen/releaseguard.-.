package com.releaseguard.release;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseRepository extends JpaRepository<Release, Long> {

        boolean existsByApplication_IdAndVersion(
                        Long applicationId,
                        String version);

        Optional<Release> findByIdAndApplication_Id(
                        Long releaseId,
                        Long applicationId);

        Page<Release> findByApplication_Id(
                        Long applicationId,
                        Pageable pageable);
}