package org.serviceproject.academic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.academic.dto.AcademicYearResponse;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.repository.AcademicYearRepository;
import org.serviceproject.common.config.AppProperties;
import org.serviceproject.common.exception.AppException;
import org.springframework.http.HttpStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicYearServiceTest {

    @Mock
    private AcademicYearRepository academicYearRepository;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private AcademicYearService academicYearService;

    private AcademicYear currentYear;

    @BeforeEach
    void setUp() {
        currentYear = new AcademicYear("2026/2027",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 8, 31),
                true);
        currentYear.setId(1L);
    }

    @Test
    void findAll_returnsList() {
        when(academicYearRepository.findAllByOrderByStartDateDesc())
                .thenReturn(List.of(currentYear));

        List<AcademicYearResponse> result = academicYearService.findAll();

        assertEquals(1, result.size());
        assertEquals("2026/2027", result.get(0).name());
        assertTrue(result.get(0).current());
    }

    @Test
    void findCurrent_success() {
        when(academicYearRepository.findByCurrentTrue()).thenReturn(Optional.of(currentYear));

        AcademicYearResponse result = academicYearService.findCurrent();

        assertNotNull(result);
        assertEquals("2026/2027", result.name());
        assertTrue(result.current());
    }

    @Test
    void findCurrent_notFound_autoCreates() {
        when(academicYearRepository.findByCurrentTrue()).thenReturn(Optional.empty());
        when(appProperties.timeZone()).thenReturn("Africa/Cairo");
        when(academicYearRepository.findByName(any())).thenReturn(Optional.of(currentYear));

        AcademicYearResponse result = academicYearService.findCurrent();
        assertNotNull(result);
        assertEquals("2026/2027", result.name());
    }

    @Test
    void getCurrentEntity_success() {
        when(academicYearRepository.findByCurrentTrue()).thenReturn(Optional.of(currentYear));

        AcademicYear entity = academicYearService.getCurrentEntity();

        assertNotNull(entity);
        assertEquals("2026/2027", entity.getName());
    }

    @Test
    void ensureAcademicYear_existing_returnsExisting() {
        LocalDate date = LocalDate.of(2026, 10, 15);
        when(academicYearRepository.findByName("2026/2027")).thenReturn(Optional.of(currentYear));

        AcademicYear result = academicYearService.ensureAcademicYear(date);

        assertEquals(currentYear, result);
        verify(academicYearRepository, never()).save(any());
    }

    @Test
    void ensureAcademicYear_new_createsAndClearsOldFlags() {
        LocalDate date = LocalDate.of(2027, 9, 1);
        when(academicYearRepository.findByName("2027/2028")).thenReturn(Optional.empty());
        when(academicYearRepository.save(any(AcademicYear.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AcademicYear result = academicYearService.ensureAcademicYear(date);

        assertNotNull(result);
        assertEquals("2027/2028", result.getName());
        assertEquals(LocalDate.of(2027, 9, 1), result.getStartDate());
        assertEquals(LocalDate.of(2028, 8, 31), result.getEndDate());
        assertTrue(result.isCurrent());

        verify(academicYearRepository).clearCurrentFlags();
        verify(academicYearRepository).save(any(AcademicYear.class));
    }
}
