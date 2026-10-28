package com.releaseguard.approval;

import com.releaseguard.application.ApplicationService;
import com.releaseguard.application.CreateApplicationRequest;
import com.releaseguard.release.CreateReleaseRequest;
import com.releaseguard.release.ReleaseRepository;
import com.releaseguard.release.ReleaseService;
import com.releaseguard.release.ReleaseStatus;
import com.releaseguard.sbom.SbomService;
import com.releaseguard.vulnerability.VulnerabilityImportService;
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
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class ReleaseDecisionPersistenceTest {

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

    @Autowired
    private VulnerabilityImportService importService;

    @Autowired
    private ReleaseDecisionService decisionService;

    @Autowired
    private ReleaseDecisionRepository decisionRepository;

    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void approvesReviewedReleaseAndRecordsExactEvidence() {
        Fixture fixture = createFixture(false);

        submit(fixture);

        var decision = decisionService.decide(
                fixture.applicationId(),
                fixture.releaseId(),
                approve(fixture.importId())
        );

        assertNotNull(decision.id());
        assertNotNull(decision.decidedAt());
        assertEquals(ReleaseDecisionAction.APPROVE, decision.action());
        assertEquals(
                ReleaseStatus.READY_FOR_REVIEW,
                decision.fromStatus()
        );
        assertEquals(ReleaseStatus.APPROVED, decision.toStatus());
        assertEquals(fixture.importId(), decision.vulnerabilityImportId());
        assertEquals("release-policy-v1", decision.policyVersion());
        assertEquals("Reviewer", decision.actor());

        assertEquals(ReleaseStatus.APPROVED, status(fixture));
        assertEquals(2L, historyCount(fixture));
    }

    @Test
    void highFindingBlocksApprovalWithoutChangingStatusOrHistory() {
        Fixture fixture = createFixture(true);

        submit(fixture);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> decisionService.decide(
                        fixture.applicationId(),
                        fixture.releaseId(),
                        approve(fixture.importId())
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(ReleaseStatus.READY_FOR_REVIEW, status(fixture));
        assertEquals(1L, historyCount(fixture));
    }

    @Test
    void rejectsScanBelongingToAnotherRelease() {
        Fixture target = createFixture(false);
        Fixture other = createFixture(false);

        submit(target);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> decisionService.decide(
                        target.applicationId(),
                        target.releaseId(),
                        approve(other.importId())
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals(ReleaseStatus.READY_FOR_REVIEW, status(target));
        assertEquals(1L, historyCount(target));
    }

    @Test
    void rollsBackReleaseStatusAndDecisionHistoryTogether() {
        Fixture fixture = createFixture(false);

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        assertThrows(
                ForcedRollback.class,
                () -> transaction.executeWithoutResult(transactionStatus -> {
                    submit(fixture);
                    throw new ForcedRollback();
                })
        );

        assertEquals(ReleaseStatus.DRAFT, status(fixture));
        assertEquals(0L, historyCount(fixture));
    }

    private void submit(Fixture fixture) {
        decisionService.decide(
                fixture.applicationId(),
                fixture.releaseId(),
                new ReleaseDecisionRequest(
                        ReleaseDecisionAction.SUBMIT,
                        "Release Team",
                        "Ready for review",
                        null
                )
        );
    }

    private ReleaseDecisionRequest approve(Long importId) {
        return new ReleaseDecisionRequest(
                ReleaseDecisionAction.APPROVE,
                "  Reviewer  ",
                "Selected scan satisfies release-policy-v1",
                importId
        );
    }

    private ReleaseStatus status(Fixture fixture) {
        return releaseRepository.findByIdAndApplication_Id(
                fixture.releaseId(),
                fixture.applicationId()
        ).orElseThrow().getStatus();
    }

    private long historyCount(Fixture fixture) {
        return decisionRepository.findByRelease_Id(
                fixture.releaseId(),
                PageRequest.of(0, 10)
        ).getTotalElements();
    }

    private Fixture createFixture(boolean highFinding) {
        var application = applicationService.create(
                new CreateApplicationRequest(
                        "Approval Test " + UUID.randomUUID(),
                        null,
                        null,
                        "Release Team"
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

        byte[] inventory = """
                {
                  "bomFormat": "CycloneDX",
                  "specVersion": "1.6",
                  "components": [
                    {
                      "bom-ref": "demo",
                      "type": "library",
                      "name": "demo-library",
                      "version": "1.0.0"
                    }
                  ]
                }
                """.getBytes(StandardCharsets.UTF_8);

        var sbom = sbomService.upload(
                application.id(),
                release.id(),
                "bom.json",
                inventory
        );

        List<Map<String, String>> findings = highFinding
                ? List.of(Map.of(
                        "componentBomRef", "demo",
                        "vulnerabilityId", "DEMO-001",
                        "source", "demo",
                        "severity", "HIGH"
                ))
                : List.of();

        byte[] report = objectMapper.writeValueAsBytes(Map.of(
                "formatVersion", "1",
                "sbomSha256", sbom.documentSha256(),
                "scannerName", "DemoScanner",
                "scannerVersion", "1.0.0",
                "scannedAt", Instant.now().minusSeconds(60).toString(),
                "findings", findings
        ));

        var scan = importService.upload(
                application.id(),
                release.id(),
                "report.json",
                report
        );

        return new Fixture(
                application.id(),
                release.id(),
                scan.id()
        );
    }

    private record Fixture(
            Long applicationId,
            Long releaseId,
            Long importId
    ) {
    }

    private static class ForcedRollback extends RuntimeException {
    }
}