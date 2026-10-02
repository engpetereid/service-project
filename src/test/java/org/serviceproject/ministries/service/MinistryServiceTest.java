package org.serviceproject.ministries.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.ministries.dto.MinistryRequest;
import org.serviceproject.ministries.dto.MinistryResponse;
import org.serviceproject.ministries.entity.Ministry;
import org.serviceproject.ministries.mapper.MinistryMapper;
import org.serviceproject.ministries.repository.MinistryRepository;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinistryServiceTest {

    @Mock
    private MinistryRepository ministryRepository;

    @Mock
    private MinistryMapper ministryMapper;

    @InjectMocks
    private MinistryService ministryService;

    private Ministry testMinistry;
    private MinistryResponse testResponse;

    @BeforeEach
    void setUp() {
        testMinistry = new Ministry("ابتدائي");
        testMinistry.setId(1L);
        testMinistry.setActive(true);

        testResponse = new MinistryResponse(1L, "ابتدائي", true);
    }

    @Test
    void findAll_returnsList() {
        when(ministryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(testMinistry));
        when(ministryMapper.toResponseList(List.of(testMinistry))).thenReturn(List.of(testResponse));

        List<MinistryResponse> result = ministryService.findAll();

        assertEquals(1, result.size());
        assertEquals("ابتدائي", result.get(0).name());
    }

    @Test
    void findActive_returnsOnlyActive() {
        when(ministryRepository.findAllByActiveTrue()).thenReturn(List.of(testMinistry));
        when(ministryMapper.toResponseList(List.of(testMinistry))).thenReturn(List.of(testResponse));

        List<MinistryResponse> result = ministryService.findActive();

        assertEquals(1, result.size());
        assertTrue(result.get(0).active());
    }

    @Test
    void findById_success() {
        when(ministryRepository.findById(1L)).thenReturn(Optional.of(testMinistry));
        when(ministryMapper.toResponse(testMinistry)).thenReturn(testResponse);

        MinistryResponse result = ministryService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
    }

    @Test
    void findById_notFound_throwsException() {
        when(ministryRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> ministryService.findById(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("MINISTRY_NOT_FOUND", ex.getCode());
    }

    @Test
    void create_success() {
        MinistryRequest request = new MinistryRequest("إعدادي");
        Ministry newMinistry = new Ministry("إعدادي");
        Ministry savedMinistry = new Ministry("إعدادي");
        savedMinistry.setId(2L);
        savedMinistry.setActive(true);
        MinistryResponse response = new MinistryResponse(2L, "إعدادي", true);

        when(ministryRepository.existsByName("إعدادي")).thenReturn(false);
        when(ministryMapper.toEntity(request)).thenReturn(newMinistry);
        when(ministryRepository.save(any(Ministry.class))).thenReturn(savedMinistry);
        when(ministryMapper.toResponse(savedMinistry)).thenReturn(response);

        MinistryResponse result = ministryService.create(request);

        assertNotNull(result);
        assertEquals("إعدادي", result.name());
        verify(ministryRepository).save(any(Ministry.class));
    }

    @Test
    void create_alreadyExists_throwsConflict() {
        MinistryRequest request = new MinistryRequest("ابتدائي");
        when(ministryRepository.existsByName("ابتدائي")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> ministryService.create(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("MINISTRY_EXISTS", ex.getCode());
        verify(ministryRepository, never()).save(any());
    }

    @Test
    void update_success() {
        MinistryRequest request = new MinistryRequest("ابتدائي بنين");
        when(ministryRepository.findById(1L)).thenReturn(Optional.of(testMinistry));
        when(ministryRepository.existsByName("ابتدائي بنين")).thenReturn(false);
        when(ministryRepository.save(testMinistry)).thenReturn(testMinistry);
        when(ministryMapper.toResponse(testMinistry)).thenReturn(new MinistryResponse(1L, "ابتدائي بنين", true));

        MinistryResponse result = ministryService.update(1L, request);

        assertEquals("ابتدائي بنين", result.name());
        verify(ministryRepository).save(testMinistry);
    }

    @Test
    void update_duplicateName_throwsConflict() {
        MinistryRequest request = new MinistryRequest("إعدادي");
        when(ministryRepository.findById(1L)).thenReturn(Optional.of(testMinistry));
        when(ministryRepository.existsByName("إعدادي")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> ministryService.update(1L, request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("MINISTRY_EXISTS", ex.getCode());
    }

    @Test
    void deactivate_success() {
        when(ministryRepository.findById(1L)).thenReturn(Optional.of(testMinistry));

        ministryService.deactivate(1L);

        assertFalse(testMinistry.isActive());
        verify(ministryRepository).save(testMinistry);
    }
}
