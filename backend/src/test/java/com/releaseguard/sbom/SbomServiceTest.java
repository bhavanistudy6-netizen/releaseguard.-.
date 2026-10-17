package com.releaseguard.sbom;

import com.releaseguard.release.Release;
import com.releaseguard.release.ReleaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SbomServiceTest {

    private ReleaseRepository releaseRepository;
    private SbomDocumentRepository documentRepository;
    private SbomComponentRepository componentRepository;
    private CycloneDxParser parser;
    private SbomService service;

    private final byte[] content =
            "abc".getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() {
        releaseRepository = mock(ReleaseRepository.class);
        documentRepository = mock(SbomDocumentRepository.class);
        componentRepository = mock(SbomComponentRepository.class);
        parser = mock(CycloneDxParser.class);

        service = new SbomService(
                releaseRepository,
                documentRepository,
                componentRepository,
                parser
        );
    }

    @Test
    void rejectsReleaseOutsideRequestedApplication() {
        when(releaseRepository.findByIdAndApplication_Id(10L, 2L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.upload(2L, 10L, "bom.json", content)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verifyNoInteractions(
                documentRepository,
                componentRepository,
                parser
        );
    }

    @Test
    void rejectsDuplicateUploadBeforeParsing() {
        existingRelease();

        when(documentRepository.existsByRelease_Id(10L))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.upload(1L, 10L, "bom.json", content)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(parser, componentRepository);
        verify(documentRepository, never())
                .saveAndFlush(any(SbomDocument.class));
    }

    @Test
    void doesNotSaveWhenParsingFails() {
        existingRelease();

        when(parser.parse(content))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid JSON"
                ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.upload(1L, 10L, "bom.json", content)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(documentRepository, never())
                .saveAndFlush(any(SbomDocument.class));

        verifyNoInteractions(componentRepository);
    }

    @Test
    void rejectsInvalidFilenameBeforeParsing() {
        existingRelease();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.upload(1L, 10L, "   ", content)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(parser, componentRepository);

        verify(documentRepository, never())
                .saveAndFlush(any(SbomDocument.class));
    }

    @Test
    void savesDocumentAndComponentsWithChecksum() {
        Release release = existingRelease();

        ParsedComponent component = new ParsedComponent(
                "library-ref",
                "library",
                "org.example",
                "example-library",
                "1.0.0",
                "pkg:maven/org.example/example-library@1.0.0"
        );

        when(parser.parse(content))
                .thenReturn(new ParsedSbom(
                        "CycloneDX",
                        "1.6",
                        List.of(component)
                ));

        when(documentRepository.saveAndFlush(any(SbomDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SbomUploadResponse response = service.upload(
                1L,
                10L,
                "C:\\uploads\\bom.json",
                content
        );

        assertEquals(Long.valueOf(10L), response.releaseId());
        assertEquals("bom.json", response.originalFilename());
        assertEquals("CycloneDX", response.bomFormat());
        assertEquals("1.6", response.specVersion());
        assertEquals(1, response.componentCount());

        // Known SHA-256 value for the bytes "abc".
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223"
                        + "b00361a396177a9cb410ff61f20015ad",
                response.documentSha256()
        );

        ArgumentCaptor<SbomDocument> documentCaptor =
                ArgumentCaptor.forClass(SbomDocument.class);

        verify(documentRepository)
                .saveAndFlush(documentCaptor.capture());

        SbomDocument savedDocument = documentCaptor.getValue();

        assertSame(release, savedDocument.getRelease());

        verify(componentRepository).saveAllAndFlush(argThat(items -> {
            var iterator = items.iterator();

            if (!iterator.hasNext()) {
                return false;
            }

            SbomComponent savedComponent = iterator.next();

            return !iterator.hasNext()
                    && savedComponent.getSbomDocument() == savedDocument
                    && "library-ref".equals(savedComponent.getBomRef())
                    && "library".equals(savedComponent.getComponentType())
                    && "org.example".equals(savedComponent.getGroupName())
                    && "example-library".equals(savedComponent.getName())
                    && "1.0.0".equals(savedComponent.getVersion())
                    && component.purl().equals(savedComponent.getPurl());
        }));
    }

    private Release existingRelease() {
        Release release = mock(Release.class);

        when(releaseRepository.findByIdAndApplication_Id(10L, 1L))
                .thenReturn(Optional.of(release));

        return release;
    }
}