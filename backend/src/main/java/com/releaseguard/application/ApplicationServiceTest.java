package com.releaseguard.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationServiceTest {

    private ApplicationRepository repository;
    private ApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(ApplicationRepository.class);
        service = new ApplicationService(repository);
    }

    @Test
    void missingApplicationReturns404() {
        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getById(999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void invalidPaginationReturns400WithoutQueryingDatabase() {
        int[][] invalidInputs = {
                {-1, 10},
                {0, 0},
                {0, 101}
        };

        for (int[] input : invalidInputs) {
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> service.list(input[0], input[1])
            );

            assertEquals(
                    HttpStatus.BAD_REQUEST,
                    exception.getStatusCode()
            );
        }

        verifyNoInteractions(repository);
    }

    @Test
    void emptyPageReturnsCorrectPaginationDetails() {
        PageRequest pageable = PageRequest.of(
                0,
                10,
                Sort.by("id").ascending()
        );

        when(repository.findAll(any(Pageable.class)))
                .thenReturn(Page.empty(pageable));

        ApplicationPageResponse response = service.list(0, 10);

        assertTrue(response.content().isEmpty());
        assertEquals(0, response.page());
        assertEquals(10, response.size());
        assertEquals(0L, response.totalElements());
        assertEquals(0, response.totalPages());

        verify(repository).findAll(pageable);
    }
}