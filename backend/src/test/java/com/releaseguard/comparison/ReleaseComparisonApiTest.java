package com.releaseguard.comparison;

import com.releaseguard.application.ApplicationService;
import com.releaseguard.application.CreateApplicationRequest;
import com.releaseguard.release.CreateReleaseRequest;
import com.releaseguard.release.ReleaseService;
import com.releaseguard.sbom.SbomService;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ReleaseComparisonApiTest {

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
    private Environment environment;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void comparesPersistedInventoriesThroughHttp() throws Exception {
        Long applicationId = createApplication();

        Long oldReleaseId = createRelease(applicationId, "v1.0.0");
        Long newReleaseId = createRelease(applicationId, "v2.0.0");

        upload(applicationId, oldReleaseId, """
                [
                  {
                    "type": "library",
                    "name": "demo",
                    "version": "1.0.0",
                    "purl": "pkg:maven/org.example/demo@1.0.0"
                  },
                  {
                    "type": "library",
                    "name": "old-library",
                    "version": "1.0.0",
                    "purl": "pkg:maven/org.example/old-library@1.0.0"
                  }
                ]
                """);

        upload(applicationId, newReleaseId, """
                [
                  {
                    "type": "library",
                    "name": "demo",
                    "version": "2.0.0",
                    "purl": "pkg:maven/org.example/demo@2.0.0"
                  },
                  {
                    "type": "library",
                    "name": "new-library",
                    "version": "1.0.0",
                    "purl": "pkg:maven/org.example/new-library@1.0.0"
                  }
                ]
                """);

        var response = compare(
                applicationId,
                oldReleaseId,
                newReleaseId
        );

        assertEquals(200, response.statusCode());

        var body = objectMapper.readTree(response.body());

        assertEquals(applicationId.longValue(),
                body.get("applicationId").longValue());
        assertEquals(oldReleaseId.longValue(),
                body.get("fromReleaseId").longValue());
        assertEquals(newReleaseId.longValue(),
                body.get("toReleaseId").longValue());
        assertEquals(3, body.get("changes").size());

        Map<String, String> classifications = new HashMap<>();

        for (var change : body.get("changes")) {
            classifications.put(
                    change.get("identity").stringValue(),
                    change.get("changeType").stringValue()
            );
        }

        assertEquals(Map.of(
                "pkg:maven/org.example/demo", "VERSION_CHANGED",
                "pkg:maven/org.example/old-library", "REMOVED",
                "pkg:maven/org.example/new-library", "ADDED"
        ), classifications);
    }

    @Test
    void rejectsReleaseOwnedByAnotherApplication() throws Exception {
        Long applicationId = createApplication();
        Long otherApplicationId = createApplication();

        Long oldReleaseId = createRelease(applicationId, "v1.0.0");
        Long foreignReleaseId = createRelease(
                otherApplicationId,
                "v2.0.0"
        );

        var response = compare(
                applicationId,
                oldReleaseId,
                foreignReleaseId
        );

        assertEquals(404, response.statusCode());
    }

    @Test
    void rejectsMissingSbomInsteadOfTreatingItAsEmpty() throws Exception {
        Long applicationId = createApplication();

        Long oldReleaseId = createRelease(applicationId, "v1.0.0");
        Long newReleaseId = createRelease(applicationId, "v2.0.0");

        upload(applicationId, oldReleaseId, "[]");

        // The new release deliberately has no uploaded SBOM.
        var response = compare(
                applicationId,
                oldReleaseId,
                newReleaseId
        );

        assertEquals(404, response.statusCode());
    }

    private Long createApplication() {
        return applicationService.create(
                new CreateApplicationRequest(
                        "Comparison Test " + UUID.randomUUID(),
                        null,
                        null,
                        "Test Team"
                )
        ).id();
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

    private void upload(
            Long applicationId,
            Long releaseId,
            String componentsJson) {

        String json = """
                {
                  "bomFormat": "CycloneDX",
                  "specVersion": "1.6",
                  "version": 1,
                  "components": %s
                }
                """.formatted(componentsJson);

        sbomService.upload(
                applicationId,
                releaseId,
                "bom.json",
                json.getBytes(StandardCharsets.UTF_8)
        );
    }

    private HttpResponse<String> compare(
            Long applicationId,
            Long fromReleaseId,
            Long toReleaseId) throws Exception {

        String port = environment.getRequiredProperty(
                "local.server.port"
        );

        URI uri = URI.create(
                "http://localhost:" + port
                        + "/api/v1/applications/" + applicationId
                        + "/releases/compare"
                        + "?fromReleaseId=" + fromReleaseId
                        + "&toReleaseId=" + toReleaseId
        );

        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
        }
    }
}