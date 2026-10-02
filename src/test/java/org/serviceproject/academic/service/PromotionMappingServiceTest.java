package org.serviceproject.academic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.academic.dto.PromotionMappingRequest;
import org.serviceproject.academic.dto.PromotionMappingResponse;
import org.serviceproject.academic.entity.PromotionMapping;
import org.serviceproject.academic.repository.PromotionMappingRepository;
import org.serviceproject.classes.entity.GradeClass;
import org.serviceproject.classes.repository.GradeClassRepository;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.ministries.entity.Ministry;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionMappingServiceTest {

    @Mock
    private PromotionMappingRepository promotionMappingRepository;

    @Mock
    private GradeClassRepository gradeClassRepository;

    @InjectMocks
    private PromotionMappingService promotionMappingService;

    private Ministry ministry;
    private GradeClass class1;
    private GradeClass class2;
    private PromotionMapping mapping;

    @BeforeEach
    void setUp() {
        ministry = new Ministry("ابتدائي");
        ministry.setId(10L);

        class1 = new GradeClass("أولى ابتدائي", ministry);
        class1.setId(100L);

        class2 = new GradeClass("ثانية ابتدائي", ministry);
        class2.setId(200L);

        mapping = new PromotionMapping();
        mapping.setId(1L);
        mapping.setSourceClass(class1);
        mapping.setTargetClass(class2);
        mapping.setGraduation(false);
    }

    @Test
    void findAll_returnsList() {
        when(promotionMappingRepository.findAllWithClasses()).thenReturn(List.of(mapping));

        List<PromotionMappingResponse> list = promotionMappingService.findAll();

        assertEquals(1, list.size());
        assertEquals(100L, list.get(0).sourceClassId());
        assertEquals(200L, list.get(0).targetClassId());
        assertFalse(list.get(0).graduation());
    }

    @Test
    void findById_success() {
        when(promotionMappingRepository.findById(1L)).thenReturn(Optional.of(mapping));

        PromotionMappingResponse res = promotionMappingService.findById(1L);

        assertNotNull(res);
        assertEquals(100L, res.sourceClassId());
    }

    @Test
    void findById_notFound_throwsException() {
        when(promotionMappingRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> promotionMappingService.findById(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void create_regularMapping_success() {
        PromotionMappingRequest request = new PromotionMappingRequest(100L, 200L, false);

        when(gradeClassRepository.findByIdWithMinistry(100L)).thenReturn(Optional.of(class1));
        when(promotionMappingRepository.existsBySourceClassId(100L)).thenReturn(false);
        when(gradeClassRepository.findByIdWithMinistry(200L)).thenReturn(Optional.of(class2));
        when(promotionMappingRepository.save(any(PromotionMapping.class))).thenAnswer(i -> {
            PromotionMapping pm = i.getArgument(0);
            pm.setId(10L);
            return pm;
        });

        PromotionMappingResponse res = promotionMappingService.create(request);

        assertNotNull(res);
        assertEquals(100L, res.sourceClassId());
        assertEquals(200L, res.targetClassId());
        assertFalse(res.graduation());
    }

    @Test
    void create_graduationMapping_success() {
        PromotionMappingRequest request = new PromotionMappingRequest(100L, null, true);

        when(gradeClassRepository.findByIdWithMinistry(100L)).thenReturn(Optional.of(class1));
        when(promotionMappingRepository.existsBySourceClassId(100L)).thenReturn(false);
        when(promotionMappingRepository.save(any(PromotionMapping.class))).thenAnswer(i -> {
            PromotionMapping pm = i.getArgument(0);
            pm.setId(11L);
            return pm;
        });

        PromotionMappingResponse res = promotionMappingService.create(request);

        assertNotNull(res);
        assertTrue(res.graduation());
        assertNull(res.targetClassId());
    }

    @Test
    void create_missingTargetClass_throwsBadRequest() {
        PromotionMappingRequest request = new PromotionMappingRequest(100L, null, false);

        when(gradeClassRepository.findByIdWithMinistry(100L)).thenReturn(Optional.of(class1));
        when(promotionMappingRepository.existsBySourceClassId(100L)).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> promotionMappingService.create(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("TARGET_CLASS_REQUIRED", ex.getCode());
    }

    @Test
    void create_sameSourceAndTarget_throwsBadRequest() {
        PromotionMappingRequest request = new PromotionMappingRequest(100L, 100L, false);

        when(gradeClassRepository.findByIdWithMinistry(100L)).thenReturn(Optional.of(class1));
        when(promotionMappingRepository.existsBySourceClassId(100L)).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> promotionMappingService.create(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_TARGET_CLASS", ex.getCode());
    }

    @Test
    void create_duplicateSource_throwsConflict() {
        PromotionMappingRequest request = new PromotionMappingRequest(100L, 200L, false);

        when(gradeClassRepository.findByIdWithMinistry(100L)).thenReturn(Optional.of(class1));
        when(promotionMappingRepository.existsBySourceClassId(100L)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> promotionMappingService.create(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("MAPPING_EXISTS", ex.getCode());
    }

    @Test
    void delete_success() {
        when(promotionMappingRepository.findById(1L)).thenReturn(Optional.of(mapping));

        promotionMappingService.delete(1L);

        verify(promotionMappingRepository).delete(mapping);
    }
}
