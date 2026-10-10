package com.releaseguard;

import com.releaseguard.application.ApplicationResponse;
import com.releaseguard.application.ApplicationService;
import com.releaseguard.application.CreateApplicationRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class BackendApplicationTests {

    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17.11");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ApplicationService service;

    @Test
    @Transactional
    void applicationCanBeSavedAndRetrieved() {
        CreateApplicationRequest request = new CreateApplicationRequest(
                "  Integration Test App  ",
                "Created by an automated test",
                null,
                "  Test Team  "
        );

        ApplicationResponse created = service.create(request);
        ApplicationResponse retrieved = service.getById(created.id());

        assertNotNull(created.id());
        assertEquals(created.id(), retrieved.id());
        assertEquals("Integration Test App", retrieved.name());
        assertEquals("Test Team", retrieved.ownerTeam());
        assertEquals(
                "Created by an automated test",
                retrieved.description()
        );
        assertNull(retrieved.repositoryUrl());
        assertNotNull(retrieved.createdAt());
        assertNotNull(retrieved.updatedAt());
    }
}