package com.releaseguard.sbom;

import com.releaseguard.application.ApplicationService;
import com.releaseguard.application.CreateApplicationRequest;
import com.releaseguard.release.CreateReleaseRequest;
import com.releaseguard.release.ReleaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class SbomPersistenceTest {

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

    @Autowired
    private SbomService sbomService;

    private static final byte[] SBOM = """
            {
              "bomFormat": "CycloneDX",
              "specVersion": "1.6",
              "version": 1,
              "components": [
                {
                  "bom-ref": "first",
                  "type": "library",
                  "name": "first-library",
                  "version": "1.0.0"
                },
                {
                  "bom-ref": "second",
                  "type": "library",
                  "name": "second-library",
                  "version": "2.0.0"
                }
              ]
            }
            """.getBytes(StandardCharsets.UTF_8);

    @Test
    void persistsInventoryAndSupportsPagination() {
        TestRelease release = createRelease();

        SbomUploadResponse uploaded = sbomService.upload(
                release.applicationId(),
                release.releaseId(),
                "bom.json",
                SBOM
        );

        assertNotNull(uploaded.id());
        assertNotNull(uploaded.uploadedAt());
        assertEquals(2, uploaded.componentCount());

        // Separate service calls read the committed inventory.
        var firstPage = sbomService.listComponents(
                release.applicationId(),
                release.releaseId(),
                0,
                1
        );

        var secondPage = sbomService.listComponents(
                release.applicationId(),
                release.releaseId(),
                1,
                1
        );

        assertEquals(2L, firstPage.totalElements());
        assertEquals(2, firstPage.totalPages());
        assertEquals(1, firstPage.content().size());
        assertEquals(
                "first-library",
                firstPage.content().getFirst().name()
        );
        assertEquals(
                "second-library",
                secondPage.content().getFirst().name()
        );
        assertEquals(
                "2.0.0",
                secondPage.content().getFirst().version()
        );
    }

    @Test
    void duplicateUploadDoesNotChangeInventory() {
        TestRelease release = createRelease();

        sbomService.upload(
                release.applicationId(),
                release.releaseId(),
                "bom.json",
                SBOM
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sbomService.upload(
                        release.applicationId(),
                        release.releaseId(),
                        "another-bom.json",
                        SBOM
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        var inventory = sbomService.listComponents(
                release.applicationId(),
                release.releaseId(),
                0,
                10
        );

        assertEquals(2L, inventory.totalElements());
    }

    @Test
    void rejectsInventoryLookupUnderAnotherApplication() {
        TestRelease release = createRelease();

        var otherApplication = applicationService.create(
                new CreateApplicationRequest(
                        "Other " + UUID.randomUUID(),
                        null,
                        null,
                        "Other Team"
                )
        );

        sbomService.upload(
                release.applicationId(),
                release.releaseId(),
                "bom.json",
                SBOM
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sbomService.listComponents(
                        otherApplication.id(),
                        release.releaseId(),
                        0,
                        10
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void rejectsInvalidPagination() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sbomService.listComponents(1L, 1L, 0, 0)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private TestRelease createRelease() {
        var application = applicationService.create(
                new CreateApplicationRequest(
                        "SBOM Test " + UUID.randomUUID(),
                        "Temporary integration-test application",
                        null,
                        "Test Team"
                )
        );

        var release = releaseService.create(
                application.id(),
                new CreateReleaseRequest(
                        "v1.0.0",
                        "a".repeat(40),
                        "test-app:1.0.0",
                        null
                )
        );

        return new TestRelease(application.id(), release.id());
    }

    private record TestRelease(
            Long applicationId,
            Long releaseId
    ) {
    }
}