package com.releaseguard.application;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ApplicationRepository
                extends JpaRepository<Application, Long> {

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select a from Application a where a.id = :id")
        Optional<Application> findByIdForUpdate(@Param("id") Long id);
}