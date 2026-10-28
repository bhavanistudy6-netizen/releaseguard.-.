package com.releaseguard.deployment;

import com.releaseguard.application.ApplicationService;
import com.releaseguard.application.CreateApplicationRequest;
import com.releaseguard.release.CreateReleaseRequest;
import com.releaseguard.release.ReleaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class DeploymentPersistenceTest {

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
    private DeploymentService deploymentService;

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private CurrentDeploymentRepository currentRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void replacesCurrentDeploymentWhilePreservingHistory() {
        Fixture fixture = createApplication();

        var first = deploymentService.record(
                fixture.applicationId(),
                request(fixture.firstReleaseId(), DeploymentEnvironment.STAGING)
        );

        CurrentDeploymentId key = new CurrentDeploymentId(
                fixture.applicationId(),
                DeploymentEnvironment.STAGING
        );

        var initial = currentRepository.findById(key).orElseThrow();

        assertEquals(first.id(), initial.getDeployment().getId());
        assertNotNull(initial.getUpdatedAt());
        assertNotNull(initial.getVersion());

        Long initialVersion = initial.getVersion();

        var second = deploymentService.record(
                fixture.applicationId(),
                request(fixture.secondReleaseId(), DeploymentEnvironment.STAGING)
        );

        var current = currentRepository.findById(key).orElseThrow();

        assertEquals(second.id(), current.getDeployment().getId());
        assertTrue(current.getVersion() > initialVersion);

        var history = deploymentRepository.findByApplication_IdAndEnvironment(
                fixture.applicationId(),
                DeploymentEnvironment.STAGING,
                PageRequest.of(0, 10)
        );

        assertEquals(2L, history.getTotalElements());
        assertTrue(deploymentRepository.existsById(first.id()));
        assertTrue(deploymentRepository.existsById(second.id()));

        assertEquals("Deploy Team", second.deployedBy());
        assertEquals("v2.0.0", second.releaseVersion());
        assertNotNull(second.recordedAt());
    }

        @Test
    void keepsEnvironmentPointersSeparate() {
        Fixture fixture = createApplication();

        var staging = deploymentService.record(
                fixture.applicationId(),
                request(
                        fixture.firstReleaseId(),
                        DeploymentEnvironment.STAGING));

        var development = deploymentService.record(
                fixture.applicationId(),
                request(
                        fixture.secondReleaseId(),
                        DeploymentEnvironment.DEVELOPMENT));

        var stagingPointer = currentRepository.findById(
                new CurrentDeploymentId(
                        fixture.applicationId(),
                        DeploymentEnvironment.STAGING))
                .orElseThrow();

        var developmentPointer = currentRepository.findById(
                new CurrentDeploymentId(
                        fixture.applicationId(),
                        DeploymentEnvironment.DEVELOPMENT))
                .orElseThrow();

        assertEquals(
                staging.id(),
                stagingPointer.getDeployment().getId());

        assertEquals(
                development.id(),
                developmentPointer.getDeployment().getId());

        assertEquals(
                2,
                currentRepository
                        .findById_ApplicationId(fixture.applicationId())
                        .size());
    }

    @Test
    void rejectsReleaseFromAnotherApplicationWithoutSaving() {
        Fixture first = createApplication();
        Fixture other = createApplication();

        long historyBefore = deploymentRepository.count();
        long pointersBefore = currentRepository.count();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> deploymentService.record(
                        first.applicationId(),
                        request(
                                other.firstReleaseId(),
                                DeploymentEnvironment.PRODUCTION
                        )
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals(historyBefore, deploymentRepository.count());
        assertEquals(pointersBefore, currentRepository.count());
    }

    @Test
    void rollsBackHistoryAndPointerUpdateTogether() {
        Fixture fixture = createApplication();

        var first = deploymentService.record(
                fixture.applicationId(),
                request(fixture.firstReleaseId(), DeploymentEnvironment.STAGING)
        );

        long historyBefore = deploymentRepository.count();

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        assertThrows(
                ForcedRollback.class,
                () -> transaction.executeWithoutResult(status -> {
                    deploymentService.record(
                            fixture.applicationId(),
                            request(
                                    fixture.secondReleaseId(),
                                    DeploymentEnvironment.STAGING
                            )
                    );

                    throw new ForcedRollback();
                })
        );

        var current = currentRepository.findById(
                new CurrentDeploymentId(
                        fixture.applicationId(),
                        DeploymentEnvironment.STAGING
                )
        ).orElseThrow();

        assertEquals(first.id(), current.getDeployment().getId());
        assertEquals(historyBefore, deploymentRepository.count());
    }

    private Fixture createApplication() {
        var application = applicationService.create(
                new CreateApplicationRequest(
                        "Deployment Test " + UUID.randomUUID(),
                        null,
                        null,
                        "Deploy Team"
                )
        );

        Long firstReleaseId = createRelease(application.id(), "v1.0.0");
        Long secondReleaseId = createRelease(application.id(), "v2.0.0");

        return new Fixture(
                application.id(),
                firstReleaseId,
                secondReleaseId
        );
    }

    private Long createRelease(Long applicationId, String version) {
        return releaseService.create(
                applicationId,
                new CreateReleaseRequest(
                        version,
                        "a".repeat(40),
                        "test-app:" + version,
                        null
                )
        ).id();
    }

    private RecordDeploymentRequest request(
            Long releaseId,
            DeploymentEnvironment environment) {

        return new RecordDeploymentRequest(
                releaseId,
                environment,
                "  Deploy Team  ",
                Instant.parse("2026-10-18T05:00:00Z"),
                "Synthetic deployment record"
        );
    }

    private record Fixture(
            Long applicationId,
            Long firstReleaseId,
            Long secondReleaseId
    ) {
    }

    private static class ForcedRollback extends RuntimeException {
    }
}