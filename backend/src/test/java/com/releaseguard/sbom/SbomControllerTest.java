package com.releaseguard.sbom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.AdditionalMatchers.aryEq;

class SbomControllerTest {

    private static final String URL =
            "/api/v1/applications/1/releases/1/sbom";

    private SbomService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(SbomService.class);

        mvc = MockMvcBuilders
                .standaloneSetup(new SbomController(service))
                .build();
    }

    @Test
    void uploadReturnsCreatedResponse() throws Exception {
        byte[] content = "{}".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bom.json",
                "application/json",
                content
        );

        SbomUploadResponse response = new SbomUploadResponse(
                7L,
                1L,
                "bom.json",
                "CycloneDX",
                "1.6",
                "a".repeat(64),
                2,
                Instant.parse("2026-10-15T12:00:00Z")
        );

        when(service.upload(
                eq(1L),
                eq(1L),
                eq("bom.json"),
                aryEq(content)
        )).thenReturn(response);

        mvc.perform(multipart(URL).file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.releaseId").value(1))
                .andExpect(jsonPath("$.componentCount").value(2))
                .andExpect(jsonPath("$.bomFormat").value("CycloneDX"));

        verify(service).upload(
                eq(1L),
                eq(1L),
                eq("bom.json"),
                aryEq(content)
        );
    }

    @Test
    void missingFileReturnsBadRequest() throws Exception {
        mvc.perform(multipart(URL))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void emptyFileReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bom.json",
                "application/json",
                new byte[0]
        );

        mvc.perform(multipart(URL).file(file))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void oversizedFileReturnsPayloadTooLarge() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bom.json",
                "application/json",
                new byte[10 * 1024 * 1024 + 1]
        );

        mvc.perform(multipart(URL).file(file))
                .andExpect(status().isPayloadTooLarge());

        verifyNoInteractions(service);
    }

    @Test
    void duplicateUploadReturnsConflict() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bom.json",
                "application/json",
                "{}".getBytes(StandardCharsets.UTF_8)
        );

        when(service.upload(
                eq(1L),
                eq(1L),
                eq("bom.json"),
                any(byte[].class)
        )).thenThrow(new ResponseStatusException(
                HttpStatus.CONFLICT,
                "An SBOM already exists for this release"
        ));

        mvc.perform(multipart(URL).file(file))
                .andExpect(status().isConflict());
    }

    @Test
    void getSbomReturnsOk() throws Exception {
        SbomUploadResponse response = new SbomUploadResponse(
                7L,
                1L,
                "bom.json",
                "CycloneDX",
                "1.6",
                "a".repeat(64),
                42,
                Instant.parse("2026-10-15T12:00:00Z")
        );

        when(service.getSbom(1L, 1L)).thenReturn(response);

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.originalFilename").value("bom.json"))
                .andExpect(jsonPath("$.componentCount").value(42));

        verify(service).getSbom(1L, 1L);
    }

    @Test
    void getSbomNotFoundReturns404() throws Exception {
        when(service.getSbom(1L, 1L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No SBOM has been uploaded for this release"));

        mvc.perform(get(URL))
                .andExpect(status().isNotFound());

        verify(service).getSbom(1L, 1L);
    }
}
