package com.releaseguard.release;

import com.releaseguard.application.ApplicationService;
import com.releaseguard.application.CreateApplicationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class ReleasePersistenceTest {

    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17.11");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ReleaseService releaseService;

    @Test
    void savesAndRetrievesReleaseFromDatabase() {
        var application = applicationService.create(
                new CreateApplicationRequest(
                        "Release Integration App",
                        "Application used for automated testing",
                        "https://github.com/example/integration-app",
                        "Integration Team"
                )
        );

        String commitSha = "a".repeat(40);
        String imageDigest = "sha256:" + "b".repeat(64);

        var created = releaseService.create(
                application.id(),
                new CreateReleaseRequest(
                        "v1.0.0",
                        commitSha,
                        "integration-app:1.0.0",
                        imageDigest
                )
        );

        var retrieved = releaseService.getById(
                application.id(),
                created.id()
        );

        assertNotNull(created.id());
        assertEquals(created.id(), retrieved.id());
        assertEquals(application.id(), retrieved.applicationId());
        assertEquals("v1.0.0", retrieved.version());
        assertEquals(commitSha, retrieved.gitCommitSha());
        assertEquals("integration-app:1.0.0", retrieved.dockerImage());
        assertEquals(imageDigest, retrieved.imageDigest());
        assertEquals(ReleaseStatus.DRAFT, retrieved.status());
        assertNotNull(retrieved.createdAt());
        assertNotNull(retrieved.updatedAt());

        var page = releaseService.list(application.id(), 0, 10);

        assertEquals(1L, page.totalElements());
        assertEquals(1, page.totalPages());
        assertEquals(1, page.content().size());
        assertEquals(retrieved.id(), page.content().getFirst().id());
    }
}