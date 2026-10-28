package com.releaseguard.approval;

import com.releaseguard.application.ApplicationRepository;
import com.releaseguard.release.Release;
import com.releaseguard.release.ReleaseRepository;
import com.releaseguard.sbom.SbomDocumentRepository;
import com.releaseguard.vulnerability.VulnerabilityFindingRepository;
import com.releaseguard.vulnerability.VulnerabilityImportRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReleaseDecisionHistoryControllerTest {

    private static final String URL = "/api/v1/applications/{applicationId}"
            + "/releases/{releaseId}/decisions";

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ReleaseRepository releaseRepository;

    @Mock
    private SbomDocumentRepository documentRepository;

    @Mock
    private VulnerabilityImportRepository importRepository;

    @Mock
    private VulnerabilityFindingRepository findingRepository;

    @Mock
    private ReleaseDecisionRepository decisionRepository;

    @Mock
    private ReleaseApprovalPolicy policy;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ReleaseDecisionService service = new ReleaseDecisionService(
                applicationRepository,
                releaseRepository,
                documentRepository,
                importRepository,
                findingRepository,
                decisionRepository,
                policy);

        mvc = MockMvcBuilders
                .standaloneSetup(new ReleaseDecisionController(service))
                .build();
    }

    @Test
    void returnsEmptyHistoryWithDefaultPagination() throws Exception {
        when(releaseRepository.findByIdAndApplication_Id(10L, 1L))
                .thenReturn(Optional.of(mock(Release.class)));

        when(decisionRepository.findByRelease_Id(
                eq(10L), any(Pageable.class)))
                .thenAnswer(invocation -> Page.<ReleaseDecision>empty(
                        invocation.getArgument(1, Pageable.class)));

        mvc.perform(get(URL, 1L, 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void passesRequestedPaginationAndNewestFirstSort() throws Exception {
        when(releaseRepository.findByIdAndApplication_Id(10L, 1L))
                .thenReturn(Optional.of(mock(Release.class)));

        when(decisionRepository.findByRelease_Id(
                eq(10L), any(Pageable.class)))
                .thenAnswer(invocation -> Page.<ReleaseDecision>empty(
                        invocation.getArgument(1, Pageable.class)));

        mvc.perform(get(URL, 1L, 10L)
                .param("page", "2")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        verify(decisionRepository)
                .findByRelease_Id(eq(10L), captor.capture());

        Pageable pageable = captor.getValue();

        assertEquals(2, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());

        Sort.Order order = pageable.getSort().getOrderFor("id");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    void returns404ForMissingRelease() throws Exception {
        when(releaseRepository.findByIdAndApplication_Id(999L, 1L))
                .thenReturn(Optional.empty());

        mvc.perform(get(URL, 1L, 999L))
                .andExpect(status().isNotFound());

        verifyNoInteractions(decisionRepository);
    }

    @Test
    void returns404WhenReleaseDoesNotBelongToApplication()
            throws Exception {

        when(releaseRepository.findByIdAndApplication_Id(10L, 2L))
                .thenReturn(Optional.empty());

        mvc.perform(get(URL, 2L, 10L))
                .andExpect(status().isNotFound());

        verify(releaseRepository)
                .findByIdAndApplication_Id(10L, 2L);

        verifyNoInteractions(decisionRepository);
    }

    @ParameterizedTest
    @CsvSource({
            "-1,10",
            "0,0",
            "0,101"
    })
    void rejectsInvalidPagination(int page, int size) throws Exception {
        mvc.perform(get(URL, 1L, 10L)
                .param("page", Integer.toString(page))
                .param("size", Integer.toString(size)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(releaseRepository, decisionRepository);
    }
}