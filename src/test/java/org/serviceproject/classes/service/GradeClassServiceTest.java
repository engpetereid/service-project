package org.serviceproject.classes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.classes.dto.GradeClassRequest;
import org.serviceproject.classes.dto.GradeClassResponse;
import org.serviceproject.classes.entity.GradeClass;
import org.serviceproject.classes.repository.GradeClassRepository;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.ministries.entity.Ministry;
import org.serviceproject.ministries.repository.MinistryRepository;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradeClassServiceTest {

    @Mock
    private GradeClassRepository gradeClassRepository;

    @Mock
    private MinistryRepository ministryRepository;

    @InjectMocks
    private GradeClassService gradeClassService;

    private Ministry ministry;
    private GradeClass gradeClass;

    @BeforeEach
    void setUp() {
        ministry = new Ministry("ابتدائي");
        ministry.setId(10L);

        gradeClass = new GradeClass("أولى ابتدائي", ministry);
        gradeClass.setId(1L);
        gradeClass.setActive(true);
        gradeClass.setSortOrder(1);
    }

    @Test
    void findAll_returnsList() {
        when(gradeClassRepository.findAllByActiveTrueOrderBySortOrderAsc())
                .thenReturn(List.of(gradeClass));

        List<GradeClassResponse> result = gradeClassService.findAll();

        assertEquals(1, result.size());
        assertEquals("أولى ابتدائي", result.get(0).name());
        assertEquals(10L, result.get(0).ministryId());
        assertEquals("ابتدائي", result.get(0).ministryName());
    }

    @Test
    void findByMinistry_returnsClassesForMinistry() {
        when(gradeClassRepository.findAllByMinistryIdAndActiveTrueOrderBySortOrderAsc(10L))
                .thenReturn(List.of(gradeClass));

        List<GradeClassResponse> result = gradeClassService.findByMinistry(10L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).ministryId());
    }

    @Test
    void findById_success() {
        when(gradeClassRepository.findByIdWithMinistry(1L)).thenReturn(Optional.of(gradeClass));

        GradeClassResponse result = gradeClassService.findById(1L);

        assertNotNull(result);
        assertEquals("أولى ابتدائي", result.name());
    }

    @Test
    void findById_notFound_throwsException() {
        when(gradeClassRepository.findByIdWithMinistry(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> gradeClassService.findById(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("CLASS_NOT_FOUND", ex.getCode());
    }

    @Test
    void create_success() {
        GradeClassRequest request = new GradeClassRequest("ثانية ابتدائي", 10L, 2);
        when(ministryRepository.findById(10L)).thenReturn(Optional.of(ministry));
        when(gradeClassRepository.existsByMinistryIdAndName(10L, "ثانية ابتدائي")).thenReturn(false);

        GradeClass saved = new GradeClass("ثانية ابتدائي", ministry);
        saved.setId(2L);
        saved.setSortOrder(2);
        when(gradeClassRepository.save(any(GradeClass.class))).thenReturn(saved);

        GradeClassResponse response = gradeClassService.create(request);

        assertNotNull(response);
        assertEquals("ثانية ابتدائي", response.name());
        assertEquals(10L, response.ministryId());
        verify(gradeClassRepository).save(any(GradeClass.class));
    }

    @Test
    void create_ministryNotFound_throwsNotFound() {
        GradeClassRequest request = new GradeClassRequest("ثانية ابتدائي", 99L, 2);
        when(ministryRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> gradeClassService.create(request));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("MINISTRY_NOT_FOUND", ex.getCode());
    }

    @Test
    void create_duplicateClassNameInSameMinistry_throwsConflict() {
        GradeClassRequest request = new GradeClassRequest("أولى ابتدائي", 10L, 1);
        when(ministryRepository.findById(10L)).thenReturn(Optional.of(ministry));
        when(gradeClassRepository.existsByMinistryIdAndName(10L, "أولى ابتدائي")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> gradeClassService.create(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("CLASS_EXISTS", ex.getCode());
    }

    @Test
    void update_success() {
        GradeClassRequest request = new GradeClassRequest("أولى ابتدائي (أ)", 10L, 1);
        when(gradeClassRepository.findByIdWithMinistry(1L)).thenReturn(Optional.of(gradeClass));
        when(ministryRepository.findById(10L)).thenReturn(Optional.of(ministry));
        when(gradeClassRepository.existsByMinistryIdAndName(10L, "أولى ابتدائي (أ)")).thenReturn(false);
        when(gradeClassRepository.save(any(GradeClass.class))).thenReturn(gradeClass);

        GradeClassResponse response = gradeClassService.update(1L, request);

        assertNotNull(response);
        assertEquals("أولى ابتدائي (أ)", gradeClass.getName());
    }

    @Test
    void deactivate_success() {
        when(gradeClassRepository.findById(1L)).thenReturn(Optional.of(gradeClass));

        gradeClassService.deactivate(1L);

        assertFalse(gradeClass.isActive());
        verify(gradeClassRepository).save(gradeClass);
    }
}
