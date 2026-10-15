package com.releaseguard.release;

import com.releaseguard.application.Application;
import com.releaseguard.application.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReleaseServiceTest {

        private ReleaseRepository releaseRepository;
        private ApplicationRepository applicationRepository;
        private ReleaseService releaseService;

        @BeforeEach
        void setUp() {
                releaseRepository = mock(ReleaseRepository.class);
                applicationRepository = mock(ApplicationRepository.class);

                releaseService = new ReleaseService(
                                releaseRepository,
                                applicationRepository);
        }

        @Test
        void createRejectsMissingApplication() {
                when(applicationRepository.findById(999L))
                                .thenReturn(Optional.empty());

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> releaseService.create(999L, request()));

                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
                verifyNoInteractions(releaseRepository);
        }

        @Test
        void createRejectsDuplicateVersionAfterTrimming() {
                Application application = new Application(
                                "College Planner",
                                "Tracks coursework",
                                null,
                                "Planner Team");

                when(applicationRepository.findById(1L))
                                .thenReturn(Optional.of(application));

                when(releaseRepository.existsByApplication_IdAndVersion(
                                1L, "v1.0.0")).thenReturn(true);

                CreateReleaseRequest duplicateRequest = new CreateReleaseRequest(
                                "  v1.0.0  ",
                                "a".repeat(40),
                                "college-planner:1.0.0",
                                null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> releaseService.create(1L, duplicateRequest));

                assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

                verify(releaseRepository, never())
                                .saveAndFlush(any(Release.class));
        }

        @Test
        void createRegistersDraftReleaseWithTrimmedValues() {
                Application application = mock(Application.class);

                when(application.getId()).thenReturn(1L);

                when(applicationRepository.findById(1L))
                                .thenReturn(Optional.of(application));

                when(releaseRepository.existsByApplication_IdAndVersion(
                                1L, "v1.0.0")).thenReturn(false);

                when(releaseRepository.saveAndFlush(any(Release.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                CreateReleaseRequest createRequest = new CreateReleaseRequest(
                                "  v1.0.0  ",
                                "a".repeat(40),
                                "  college-planner:1.0.0  ",
                                null);

                ReleaseResponse response = releaseService.create(
                                1L,
                                createRequest);

                assertEquals(Long.valueOf(1L), response.applicationId());
                assertEquals("v1.0.0", response.version());
                assertEquals("a".repeat(40), response.gitCommitSha());
                assertEquals("college-planner:1.0.0", response.dockerImage());
                assertEquals(ReleaseStatus.DRAFT, response.status());
                assertNull(response.imageDigest());

                verify(releaseRepository).saveAndFlush(any(Release.class));
        }

        @Test
        void getByIdRejectsReleaseOutsideRequestedApplication() {
                when(releaseRepository.findByIdAndApplication_Id(1L, 2L))
                                .thenReturn(Optional.empty());

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> releaseService.getById(2L, 1L));

                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

                verify(releaseRepository)
                                .findByIdAndApplication_Id(1L, 2L);
        }

        @Test
        void listRejectsInvalidPaginationBeforeAccessingDatabase() {
                int[][] invalidInputs = {
                                { -1, 10 },
                                { 0, 0 },
                                { 0, 101 }
                };

                for (int[] input : invalidInputs) {
                        ResponseStatusException exception = assertThrows(
                                        ResponseStatusException.class,
                                        () -> releaseService.list(
                                                        1L,
                                                        input[0],
                                                        input[1]));

                        assertEquals(
                                        HttpStatus.BAD_REQUEST,
                                        exception.getStatusCode());
                }

                verifyNoInteractions(
                                applicationRepository,
                                releaseRepository);
        }

        @Test
        void listRejectsMissingApplication() {
                when(applicationRepository.existsById(999L))
                                .thenReturn(false);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> releaseService.list(999L, 0, 10));

                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
                verifyNoInteractions(releaseRepository);
        }

        private CreateReleaseRequest request() {
                return new CreateReleaseRequest(
                                "v1.0.0",
                                "a".repeat(40),
                                "college-planner:1.0.0",
                                null);
        }
}