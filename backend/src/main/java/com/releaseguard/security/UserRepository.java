package com.releaseguard.security;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT COUNT(u) > 0 FROM User u JOIN u.roles r WHERE r = :role AND u.enabled = true")
    boolean existsByRoleAndEnabled(@Param("role") Role role);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r = com.releaseguard.security.Role.ADMIN AND u.enabled = true")
    long countActiveAdmins();

    Page<User> findAll(Pageable pageable);
}

