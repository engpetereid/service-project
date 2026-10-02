package org.serviceproject.academic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.academic.dto.PromotionRunResponse;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.entity.PromotionMapping;
import org.serviceproject.academic.entity.PromotionRun;
import org.serviceproject.academic.entity.PromotionStatus;
import org.serviceproject.academic.repository.AcademicYearRepository;
import org.serviceproject.academic.repository.PromotionMappingRepository;
import org.serviceproject.academic.repository.PromotionRunRepository;
import org.serviceproject.classes.entity.GradeClass;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.ministries.entity.Ministry;
import org.serviceproject.staff.entity.StaffPlacement;
import org.serviceproject.staff.repository.StaffPlacementRepository;
import org.serviceproject.students.entity.StudentPlacement;
import org.serviceproject.students.entity.StudentStatus;
import org.serviceproject.students.repository.StudentPlacementRepository;
import org.serviceproject.users.entity.Gender;
import org.serviceproject.users.entity.Person;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private PromotionRunRepository promotionRunRepository;

    @Mock
    private PromotionMappingRepository promotionMappingRepository;

    @Mock
    private AcademicYearRepository academicYearRepository;

    @Mock
    private StudentPlacementRepository studentPlacementRepository;

    @Mock
    private StaffPlacementRepository staffPlacementRepository;

    @InjectMocks
    private PromotionService promotionService;

    private AcademicYear prevYear;
    private AcademicYear nextYear;
    private Ministry ministry;
    private GradeClass class1;
    private GradeClass class2;
    private PromotionMapping mappingClass1To2;
    private PromotionMapping mappingClass2Graduation;

    private Person student1;
    private Person student2;
    private Person student3;
    private Person servant1;

    private StudentPlacement studentPlacement1;
    private StudentPlacement studentPlacement2;
    private StudentPlacement studentPlacement3;
    private StaffPlacement staffPlacement1;

    @BeforeEach
    void setUp() {
        prevYear = new AcademicYear("2025/2026", LocalDate.of(2025, 9, 1), LocalDate.of(2026, 8, 31), false);
        prevYear.setId(1L);

        nextYear = new AcademicYear("2026/2027", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 8, 31), true);
        nextYear.setId(2L);

        ministry = new Ministry("ابتدائي");
        ministry.setId(10L);

        class1 = new GradeClass("أولى ابتدائي", ministry);
        class1.setId(100L);

        class2 = new GradeClass("ثانية ابتدائي", ministry);
        class2.setId(200L);

        mappingClass1To2 = new PromotionMapping();
        mappingClass1To2.setSourceClass(class1);
        mappingClass1To2.setTargetClass(class2);
        mappingClass1To2.setGraduation(false);

        mappingClass2Graduation = new PromotionMapping();
        mappingClass2Graduation.setSourceClass(class2);
        mappingClass2Graduation.setTargetClass(null);
        mappingClass2Graduation.setGraduation(true);

        student1 = new Person();
        student1.setId(5001L);
        student1.setFullName("مارك");
        student1.setPhone("01111111111");
        student1.setGender(Gender.MALE);

        student2 = new Person();
        student2.setId(5002L);
        student2.setFullName("مارينا");
        student2.setPhone("01122222222");
        student2.setGender(Gender.FEMALE);

        student3 = new Person();
        student3.setId(5003L);
        student3.setFullName("كيرلس");
        student3.setPhone("01133333333");
        student3.setGender(Gender.MALE);

        servant1 = new Person();
        servant1.setId(3001L);
        servant1.setFullName("مينا جرجس");
        servant1.setPhone("01001111111");
        servant1.setGender(Gender.MALE);

        studentPlacement1 = new StudentPlacement(student1, prevYear, ministry, class1);
        studentPlacement1.setResponsibleServant(servant1);
        studentPlacement1.setGuardianPhone("01234567890");

        studentPlacement2 = new StudentPlacement(student2, prevYear, ministry, class2);

        GradeClass unmappedClass = new GradeClass("فصل غير مسكن", ministry);
        unmappedClass.setId(999L);
        studentPlacement3 = new StudentPlacement(student3, prevYear, ministry, unmappedClass);

        staffPlacement1 = new StaffPlacement(servant1, prevYear, ministry, class1);
    }

    @Test
    void executePromotion_idempotent_skipsIfAlreadyCompleted() {
        PromotionRun completedRun = new PromotionRun(nextYear);
        completedRun.markCompleted(10, 5, 0);

        when(promotionRunRepository.findByAcademicYearId(2L)).thenReturn(Optional.of(completedRun));

        PromotionRunResponse response = promotionService.executePromotion(nextYear);

        assertNotNull(response);
        assertEquals("COMPLETED", response.status());
        assertEquals(10, response.promotedCount());
        verify(studentPlacementRepository, never()).save(any());
    }

    @Test
    void executePromotion_noPreviousYear_completesWithZeroCounts() {
        when(promotionRunRepository.findByAcademicYearId(2L)).thenReturn(Optional.empty());
        when(promotionRunRepository.save(any(PromotionRun.class))).thenAnswer(i -> i.getArgument(0));
        when(academicYearRepository.findAllByOrderByStartDateDesc()).thenReturn(List.of(nextYear));

        PromotionRunResponse response = promotionService.executePromotion(nextYear);

        assertNotNull(response);
        assertEquals("COMPLETED", response.status());
        assertEquals(0, response.promotedCount());
        assertEquals(0, response.graduatedCount());
    }

    @Test
    void executePromotion_success_promotesGraduatesCopiesStaff() {
        when(promotionRunRepository.findByAcademicYearId(2L)).thenReturn(Optional.empty());
        when(promotionRunRepository.save(any(PromotionRun.class))).thenAnswer(i -> i.getArgument(0));
        when(academicYearRepository.findAllByOrderByStartDateDesc()).thenReturn(List.of(nextYear, prevYear));

        when(studentPlacementRepository.findAllByAcademicYearIdAndStatus(1L, StudentStatus.ACTIVE))
                .thenReturn(List.of(studentPlacement1, studentPlacement2, studentPlacement3));

        when(studentPlacementRepository.existsByPersonIdAndAcademicYearId(anyLong(), eq(2L))).thenReturn(false);

        when(promotionMappingRepository.findBySourceClassId(100L)).thenReturn(Optional.of(mappingClass1To2));
        when(promotionMappingRepository.findBySourceClassId(200L)).thenReturn(Optional.of(mappingClass2Graduation));
        when(promotionMappingRepository.findBySourceClassId(999L)).thenReturn(Optional.empty()); // unmapped

        when(staffPlacementRepository.findAllByAcademicYearId(1L)).thenReturn(List.of(staffPlacement1));
        when(staffPlacementRepository.existsByPersonIdAndAcademicYearId(3001L, 2L)).thenReturn(false);

        PromotionRunResponse response = promotionService.executePromotion(nextYear);

        assertNotNull(response);
        assertEquals("COMPLETED", response.status());
        assertEquals(1, response.promotedCount()); // student 1 promoted to class 2
        assertEquals(1, response.graduatedCount()); // student 2 graduated
        assertEquals(1, response.skippedCount()); // student 3 unmapped class

        // Verify promoted student placement saved
        verify(studentPlacementRepository).save(argThat(sp ->
                sp.getPerson().getId().equals(5001L) &&
                sp.getStatus() == StudentStatus.ACTIVE &&
                sp.getGradeClass().getId().equals(200L) &&
                sp.getResponsibleServant() == null && // servant reset
                "01234567890".equals(sp.getGuardianPhone())
        ));

        // Verify graduated student placement saved
        verify(studentPlacementRepository).save(argThat(sp ->
                sp.getPerson().getId().equals(5002L) &&
                sp.getStatus() == StudentStatus.GRADUATED &&
                sp.getGradeClass() == null
        ));

        // Verify staff placement copied forward
        verify(staffPlacementRepository).save(argThat(sp ->
                sp.getPerson().getId().equals(3001L) &&
                sp.getAcademicYear().getId().equals(2L) &&
                sp.getGradeClass().getId().equals(100L)
        ));
    }

    @Test
    void executePromotion_failure_marksFailedAndThrows() {
        when(promotionRunRepository.findByAcademicYearId(2L)).thenReturn(Optional.empty());
        when(promotionRunRepository.save(any(PromotionRun.class))).thenAnswer(i -> i.getArgument(0));
        when(academicYearRepository.findAllByOrderByStartDateDesc()).thenReturn(List.of(nextYear, prevYear));

        when(studentPlacementRepository.findAllByAcademicYearIdAndStatus(1L, StudentStatus.ACTIVE))
                .thenThrow(new RuntimeException("Database error"));

        AppException ex = assertThrows(AppException.class, () -> promotionService.executePromotion(nextYear));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("PROMOTION_FAILED", ex.getCode());

        verify(promotionRunRepository, atLeastOnce()).save(argThat(pr -> pr.getStatus() == PromotionStatus.FAILED));
    }
}
