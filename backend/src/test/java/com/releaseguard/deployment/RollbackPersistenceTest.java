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
class RollbackPersistenceTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.11");

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
    private RollbackService rollbackService;

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private CurrentDeploymentRepository currentRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void restoresEarlierReleaseAndPreservesHistory() {
        Fixture fixture = createFixture();

        var result = rollbackService.record(
                fixture.applicationId(),
                request(fixture, fixture.firstDeploymentId()));

        assertEquals(DeploymentType.ROLLBACK, result.deploymentType());
        assertEquals(
                fixture.secondDeploymentId(),
                result.rollbackFromDeploymentId());
        assertEquals(
                fixture.firstDeploymentId(),
                result.rollbackTargetDeploymentId());
        assertEquals(
                fixture.firstReleaseId(),
                result.deployment().releaseId());
        assertEquals("Deploy Team", result.deployment().deployedBy());
        assertEquals("Restore previous release", result.reason());

        assertNotEquals(
                fixture.firstDeploymentId(),
                result.deployment().id());

        assertEquals(
                result.deployment().id(),
                currentId(fixture));

        assertEquals(3L, historyCount(fixture));
        assertTrue(deploymentRepository.existsById(
                fixture.firstDeploymentId()));
        assertTrue(deploymentRepository.existsById(
                fixture.secondDeploymentId()));

        // Verify rollback metadata was persisted, not just returned.
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> {
                    var saved = deploymentRepository
                            .findById(result.deployment().id())
                            .orElseThrow();

                    assertEquals(
                            DeploymentType.ROLLBACK,
                            saved.getDeploymentType());
                    assertEquals(
                            fixture.secondDeploymentId(),
                            saved.getRollbackFromDeployment().getId());
                    assertEquals(
                            fixture.firstDeploymentId(),
                            saved.getRollbackTargetDeployment().getId());
                    assertEquals(
                            "Restore previous release",
                            saved.getRollbackReason());
                });
    }

    @Test
    void rejectsRepeatedRequestAfterCurrentDeploymentChanges() {
        Fixture fixture = createFixture();
        var request = request(fixture, fixture.firstDeploymentId());

        var first = rollbackService.record(
                fixture.applicationId(),
                request);

        long historyBefore = historyCount(fixture);

        var exception = assertThrows(
                ResponseStatusException.class,
                () -> rollbackService.record(
                        fixture.applicationId(),
                        request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(historyBefore, historyCount(fixture));
        assertEquals(first.deployment().id(), currentId(fixture));
    }

    @Test
    void rejectsTargetFromAnotherApplication() {
        Fixture fixture = createFixture();
        Fixture other = createFixture();

        assertRejectedWithoutChanges(
                fixture,
                other.firstDeploymentId(),
                HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsTargetFromAnotherEnvironment() {
        Fixture fixture = createFixture();

        var development = deploymentService.record(
                fixture.applicationId(),
                deploymentRequest(
                        fixture.firstReleaseId(),
                        DeploymentEnvironment.DEVELOPMENT));

        assertRejectedWithoutChanges(
                fixture,
                development.id(),
                HttpStatus.CONFLICT);
    }

    @Test
    void rollsBackHistoryAndCurrentPointerTogetherOnFailure() {
        Fixture fixture = createFixture();
        long historyBefore = historyCount(fixture);

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThrows(
                ForcedRollback.class,
                () -> transaction.executeWithoutResult(status -> {
                    rollbackService.record(
                            fixture.applicationId(),
                            request(fixture, fixture.firstDeploymentId()));

                    throw new ForcedRollback();
                }));

        assertEquals(historyBefore, historyCount(fixture));
        assertEquals(
                fixture.secondDeploymentId(),
                currentId(fixture));
    }

    private void assertRejectedWithoutChanges(
            Fixture fixture,
            Long targetId,
            HttpStatus expectedStatus) {

        long historyBefore = historyCount(fixture);
        Long currentBefore = currentId(fixture);

        var exception = assertThrows(
                ResponseStatusException.class,
                () -> rollbackService.record(
                        fixture.applicationId(),
                        request(fixture, targetId)));

        assertEquals(expectedStatus, exception.getStatusCode());
        assertEquals(historyBefore, historyCount(fixture));
        assertEquals(currentBefore, currentId(fixture));
    }

    private RecordRollbackRequest request(
            Fixture fixture,
            Long targetId) {

        return new RecordRollbackRequest(
                DeploymentEnvironment.STAGING,
                fixture.secondDeploymentId(),
                targetId,
                "  Deploy Team  ",
                Instant.now().minusSeconds(5),
                "  Restore previous release  ");
    }

    private RecordDeploymentRequest deploymentRequest(
            Long releaseId,
            DeploymentEnvironment environment) {

        return new RecordDeploymentRequest(
                releaseId,
                environment,
                "Deploy Team",
                Instant.now().minusSeconds(5),
                "Synthetic deployment for rollback testing");
    }

    private Long currentId(Fixture fixture) {
        return currentRepository.findById(
                new CurrentDeploymentId(
                        fixture.applicationId(),
                        DeploymentEnvironment.STAGING))
                .orElseThrow()
                .getDeployment()
                .getId();
    }

    private long historyCount(Fixture fixture) {
        return deploymentRepository
                .findByApplication_IdAndEnvironment(
                        fixture.applicationId(),
                        DeploymentEnvironment.STAGING,
                        PageRequest.of(0, 10))
                .getTotalElements();
    }

    private Fixture createFixture() {
        var application = applicationService.create(
                new CreateApplicationRequest(
                        "Rollback Test " + UUID.randomUUID(),
                        null,
                        null,
                        "Deploy Team"));

        Long firstReleaseId = createRelease(application.id(), "v1.0.0");
        Long secondReleaseId = createRelease(application.id(), "v2.0.0");

        var first = deploymentService.record(
                application.id(),
                deploymentRequest(
                        firstReleaseId,
                        DeploymentEnvironment.STAGING));

        var second = deploymentService.record(
                application.id(),
                deploymentRequest(
                        secondReleaseId,
                        DeploymentEnvironment.STAGING));

        return new Fixture(
                application.id(),
                firstReleaseId,
                first.id(),
                second.id());
    }

    private Long createRelease(Long applicationId, String version) {
        return releaseService.create(
                applicationId,
                new CreateReleaseRequest(
                        version,
                        "a".repeat(40),
                        "test-app:" + version,
                        null))
                .id();
    }

    private record Fixture(
            Long applicationId,
            Long firstReleaseId,
            Long firstDeploymentId,
            Long secondDeploymentId) {
    }

    private static class ForcedRollback extends RuntimeException {
    }
}