package com.releaseguard.sbom;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CycloneDxParserTest {

    private final CycloneDxParser parser =
            new CycloneDxParser(JsonMapper.builder().build());

    @Test
    void parsesComponentFields() {
        ParsedSbom result = parse("""
                {
                  "bomFormat": "CycloneDX",
                  "specVersion": "1.6",
                  "components": [
                    {
                      "bom-ref": "commons-lang3",
                      "type": "library",
                      "group": "org.apache.commons",
                      "name": "commons-lang3",
                      "version": "3.17.0",
                      "purl": "pkg:maven/org.apache.commons/commons-lang3@3.17.0"
                    }
                  ]
                }
                """);

        assertEquals("CycloneDX", result.bomFormat());
        assertEquals("1.6", result.specVersion());
        assertEquals(1, result.components().size());

        ParsedComponent component = result.components().getFirst();

        assertEquals("commons-lang3", component.bomRef());
        assertEquals("library", component.componentType());
        assertEquals("org.apache.commons", component.groupName());
        assertEquals("commons-lang3", component.name());
        assertEquals("3.17.0", component.version());
        assertEquals(
                "pkg:maven/org.apache.commons/commons-lang3@3.17.0",
                component.purl()
        );
    }

    @Test
    void includesNestedComponents() {
        ParsedSbom result = parse("""
                {
                  "bomFormat": "CycloneDX",
                  "specVersion": "1.6",
                  "components": [
                    {
                      "type": "application",
                      "name": "parent-app",
                      "components": [
                        {
                          "type": "library",
                          "name": "child-library"
                        }
                      ]
                    }
                  ]
                }
                """);

        assertEquals(2, result.components().size());
        assertEquals(
                "child-library",
                result.components().get(1).name()
        );
    }

    @Test
    void allowsMissingOptionalFields() {
        ParsedSbom result = parse("""
                {
                  "bomFormat": "CycloneDX",
                  "specVersion": "1.6",
                  "components": [
                    {
                      "type": "library",
                      "name": "unknown-version-library"
                    }
                  ]
                }
                """);

        ParsedComponent component = result.components().getFirst();

        assertNull(component.bomRef());
        assertNull(component.groupName());
        assertNull(component.version());
        assertNull(component.purl());
    }

    @Test
    void rejectsMalformedJson() {
        assertBadRequest("{ broken json");
    }

    @Test
    void rejectsUnsupportedVersion() {
        assertBadRequest("""
                {
                  "bomFormat": "CycloneDX",
                  "specVersion": "9.9",
                  "components": []
                }
                """);
    }

    @Test
    void rejectsDuplicateReferencesAcrossNesting() {
        assertBadRequest("""
                {
                  "bomFormat": "CycloneDX",
                  "specVersion": "1.6",
                  "components": [
                    {
                      "bom-ref": "same-reference",
                      "type": "application",
                      "name": "parent",
                      "components": [
                        {
                          "bom-ref": "same-reference",
                          "type": "library",
                          "name": "child"
                        }
                      ]
                    }
                  ]
                }
                """);
    }

    @Test
    void rejectsMissingComponentName() {
        assertBadRequest("""
                {
                  "bomFormat": "CycloneDX",
                  "specVersion": "1.6",
                  "components": [
                    {
                      "type": "library"
                    }
                  ]
                }
                """);
    }

    @Test
    void rejectsNonStringVersion() {
        assertBadRequest("""
                {
                  "bomFormat": "CycloneDX",
                  "specVersion": "1.6",
                  "components": [
                    {
                      "type": "library",
                      "name": "example",
                      "version": 123
                    }
                  ]
                }
                """);
    }

    @Test
    void rejectsEmptyFile() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> parser.parse(new byte[0])
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void rejectsOversizedFile() {
        byte[] content = new byte[10 * 1024 * 1024 + 1];

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> parser.parse(content)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private ParsedSbom parse(String json) {
        return parser.parse(json.getBytes(StandardCharsets.UTF_8));
    }

    private void assertBadRequest(String json) {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> parse(json)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}