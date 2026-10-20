package com.releaseguard.deployment;

import com.releaseguard.application.ApplicationService;
import com.releaseguard.application.CreateApplicationRequest;
import com.releaseguard.release.CreateReleaseRequest;
import com.releaseguard.release.ReleaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class DeploymentConcurrencyTest {

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

    @Test
    void concurrentFirstDeploymentsPreserveBothHistoryRecords()
            throws Exception {

        Fixture fixture = createFixture();

        recordConcurrentlyAndVerify(fixture, 2);
    }

    @Test
    void concurrentReplacementsPreserveExistingHistory()
            throws Exception {

        Fixture fixture = createFixture();

        deploymentService.record(
                fixture.applicationId(),
                request(fixture.firstReleaseId())
        );

        recordConcurrentlyAndVerify(fixture, 3);
    }

    private void recordConcurrentlyAndVerify(
            Fixture fixture,
            long expectedHistoryCount) throws Exception {

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        var executor = Executors.newFixedThreadPool(2);

        try {
            var first = executor.submit(() -> {
                ready.countDown();

                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Start signal timed out");
                }

                return deploymentService.record(
                        fixture.applicationId(),
                        request(fixture.firstReleaseId())
                );
            });

            var second = executor.submit(() -> {
                ready.countDown();

                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Start signal timed out");
                }

                return deploymentService.record(
                        fixture.applicationId(),
                        request(fixture.secondReleaseId())
                );
            });

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Both workers should be ready"
            );

            start.countDown();

            var firstResult = first.get(60, TimeUnit.SECONDS);
            var secondResult = second.get(60, TimeUnit.SECONDS);

            assertNotEquals(firstResult.id(), secondResult.id());

            var history =
                    deploymentRepository.findByApplication_IdAndEnvironment(
                            fixture.applicationId(),
                            DeploymentEnvironment.STAGING,
                            PageRequest.of(0, 10)
                    );

            assertEquals(expectedHistoryCount, history.getTotalElements());
            assertTrue(deploymentRepository.existsById(firstResult.id()));
            assertTrue(deploymentRepository.existsById(secondResult.id()));

            var pointers = currentRepository.findById_ApplicationId(
                    fixture.applicationId()
            );

            assertEquals(1, pointers.size());

            // The application lock is acquired before allocating
            // each deployment ID, so the later operation has the higher ID.
            Long latestId = Math.max(
                    firstResult.id(),
                    secondResult.id()
            );

            assertEquals(
                    latestId,
                    pointers.getFirst().getDeployment().getId()
            );
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private Fixture createFixture() {
        var application = applicationService.create(
                new CreateApplicationRequest(
                        "Concurrency Test " + UUID.randomUUID(),
                        null,
                        null,
                        "Deploy Team"
                )
        );

        return new Fixture(
                application.id(),
                createRelease(application.id(), "v1.0.0"),
                createRelease(application.id(), "v2.0.0")
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

    private RecordDeploymentRequest request(Long releaseId) {
        return new RecordDeploymentRequest(
                releaseId,
                DeploymentEnvironment.STAGING,
                "Deploy Team",
                Instant.parse("2026-10-18T05:00:00Z"),
                "Synthetic concurrent deployment"
        );
    }

    private record Fixture(
            Long applicationId,
            Long firstReleaseId,
            Long secondReleaseId
    ) {
    }
}