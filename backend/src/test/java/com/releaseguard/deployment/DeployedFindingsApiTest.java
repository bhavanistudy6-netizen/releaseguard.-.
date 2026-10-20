package com.releaseguard.deployment;

import com.releaseguard.application.ApplicationService;
import com.releaseguard.application.CreateApplicationRequest;
import com.releaseguard.release.CreateReleaseRequest;
import com.releaseguard.release.ReleaseService;
import com.releaseguard.sbom.SbomService;
import com.releaseguard.vulnerability.VulnerabilityFindingService;
import com.releaseguard.vulnerability.VulnerabilityImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DeployedFindingsApiTest {

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
    private SbomService sbomService;

    @Autowired
    private VulnerabilityImportService importService;

    @Autowired
    private VulnerabilityFindingService findingService;

    @Autowired
    private DeploymentService deploymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Environment environment;

    @Test
    void followsCurrentReleaseWithoutMisattributingOldFindings()
            throws Exception {

        Fixture fixture = createFixture();

        var deployment = deploymentService.record(
                fixture.applicationId(),
                request(fixture.oldReleaseId()));

        var response = getFindings(fixture);

        assertEquals(200, response.statusCode());

        var body = objectMapper.readTree(response.body());

        assertEquals(
                deployment.id().longValue(),
                body.get("deployment").get("id").longValue());

        assertEquals(
                fixture.oldReleaseId().longValue(),
                body.get("deployment").get("releaseId").longValue());

        assertEquals(
                1L,
                body.get("findings").get("totalElements").longValue());

        var finding = body.get("findings").get("content").get(0);

        assertEquals(
                "DEMO-001",
                finding.get("vulnerabilityId").stringValue());
        assertEquals(
                "Security Team",
                finding.get("ownerTeam").stringValue());

        deploymentService.record(
                fixture.applicationId(),
                request(fixture.newReleaseId()));

        // The selected scan belongs to the old release.
        var afterReplacement = getFindings(fixture);

        assertEquals(404, afterReplacement.statusCode());

        // Historical findings remain available against their own release.
        var historical = findingService.list(
                fixture.applicationId(),
                fixture.oldReleaseId(),
                fixture.importId(),
                0,
                10);

        assertEquals(1L, historical.totalElements());
    }

    @Test
    void returnsNotFoundWhenNoDeploymentIsRecorded() throws Exception {
        Fixture fixture = createFixture();

        var response = getFindings(fixture);

        assertEquals(404, response.statusCode());
    }

    private Fixture createFixture() {
        var application = applicationService.create(
                new CreateApplicationRequest(
                        "Deployed Findings " + UUID.randomUUID(),
                        null,
                        null,
                        "Security Team"));

        Long oldReleaseId = createRelease(application.id(), "v1.0.0");
        Long newReleaseId = createRelease(application.id(), "v2.0.0");

        byte[] oldInventory = """
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

        var oldSbom = sbomService.upload(
                application.id(),
                oldReleaseId,
                "old-bom.json",
                oldInventory);

        byte[] newInventory = """
                {
                  "bomFormat": "CycloneDX",
                  "specVersion": "1.6",
                  "components": []
                }
                """.getBytes(StandardCharsets.UTF_8);

        sbomService.upload(
                application.id(),
                newReleaseId,
                "new-bom.json",
                newInventory);

        byte[] report = objectMapper.writeValueAsBytes(Map.of(
                "formatVersion", "1",
                "sbomSha256", oldSbom.documentSha256(),
                "scannerName", "DemoScanner",
                "scannerVersion", "1.0.0",
                "scannedAt", "2026-10-18T05:00:00Z",
                "findings", List.of(Map.of(
                        "componentBomRef", "demo",
                        "vulnerabilityId", "DEMO-001",
                        "source", "demo",
                        "severity", "HIGH"))));

        var imported = importService.upload(
                application.id(),
                oldReleaseId,
                "report.json",
                report);

        return new Fixture(
                application.id(),
                oldReleaseId,
                newReleaseId,
                imported.id());
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

    private RecordDeploymentRequest request(Long releaseId) {
        return new RecordDeploymentRequest(
                releaseId,
                DeploymentEnvironment.STAGING,
                "Deploy Team",
                Instant.parse("2026-10-18T05:00:00Z"),
                "Synthetic deployment");
    }

    private HttpResponse<String> getFindings(Fixture fixture)
            throws Exception {

        String port = environment.getRequiredProperty(
                "local.server.port");

        URI uri = URI.create(
                "http://localhost:" + port
                        + "/api/v1/applications/" + fixture.applicationId()
                        + "/deployments/current/findings"
                        + "?environment=STAGING"
                        + "&importId=" + fixture.importId());

        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(
                    HttpRequest.newBuilder(uri).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        }
    }

    private record Fixture(
            Long applicationId,
            Long oldReleaseId,
            Long newReleaseId,
            Long importId) {
    }
}