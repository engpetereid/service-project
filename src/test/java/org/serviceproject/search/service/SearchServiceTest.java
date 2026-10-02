package org.serviceproject.search.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.service.AcademicYearService;
import org.serviceproject.classes.entity.GradeClass;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.RoleWithScope;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.ministries.entity.Ministry;
import org.serviceproject.search.dto.SearchResultResponse;
import org.serviceproject.staff.entity.StaffPlacement;
import org.serviceproject.staff.repository.StaffPlacementRepository;
import org.serviceproject.students.entity.StudentPlacement;
import org.serviceproject.students.entity.StudentStatus;
import org.serviceproject.students.repository.StudentPlacementRepository;
import org.serviceproject.users.entity.Gender;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.Role;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private StudentPlacementRepository studentPlacementRepository;

    @Mock
    private StaffPlacementRepository staffPlacementRepository;

    @Mock
    private AcademicYearService academicYearService;

    @InjectMocks
    private SearchService searchService;

    private UserPrincipal adminPrincipal;
    private UserPrincipal serviceSecretaryPrincipal;
    private UserPrincipal servantPrincipal;

    private AcademicYear currentYear;
    private Ministry ministry1;
    private Ministry ministry2;
    private GradeClass gradeClass1;
    private GradeClass gradeClass2;
    private Person servantPerson1;
    private Person servantPerson2;
    private Person studentPerson1;
    private Person studentPerson2;
    private StaffPlacement staffPlacement1;
    private StaffPlacement staffPlacement2;
    private StudentPlacement studentPlacement1;
    private StudentPlacement studentPlacement2;

    @BeforeEach
    void setUp() {
        currentYear = new AcademicYear("2026/2027", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 8, 31), true);
        currentYear.setId(1L);

        ministry1 = new Ministry("ابتدائي");
        ministry1.setId(10L);

        ministry2 = new Ministry("إعدادي");
        ministry2.setId(20L);

        gradeClass1 = new GradeClass("رابعة ابتدائي", ministry1);
        gradeClass1.setId(100L);

        gradeClass2 = new GradeClass("أولى إعدادي", ministry2);
        gradeClass2.setId(200L);

        servantPerson1 = new Person();
        servantPerson1.setId(1001L);
        servantPerson1.setFullName("مينا جرجس");
        servantPerson1.setPhone("01001111111");

        servantPerson2 = new Person();
        servantPerson2.setId(1002L);
        servantPerson2.setFullName("بيتر ماجد");
        servantPerson2.setPhone("01002222222");

        studentPerson1 = new Person();
        studentPerson1.setId(5001L);
        studentPerson1.setFullName("مارك سامح");
        studentPerson1.setPhone("01111111111");

        studentPerson2 = new Person();
        studentPerson2.setId(5002L);
        studentPerson2.setFullName("مارينا يوسف");
        studentPerson2.setPhone("01122222222");

        staffPlacement1 = new StaffPlacement(servantPerson1, currentYear, ministry1, gradeClass1);
        staffPlacement2 = new StaffPlacement(servantPerson2, currentYear, ministry2, gradeClass2);

        studentPlacement1 = new StudentPlacement(studentPerson1, currentYear, ministry1, gradeClass1);
        studentPlacement1.assignServant(servantPerson1);

        studentPlacement2 = new StudentPlacement(studentPerson2, currentYear, ministry2, gradeClass2);
        studentPlacement2.assignServant(servantPerson2);

        adminPrincipal = new UserPrincipal(1L, 999L, "01000000000", "pass", true, 0,
                Set.of(new RoleWithScope(Role.GENERAL_ADMIN, null, null)));

        serviceSecretaryPrincipal = new UserPrincipal(2L, 998L, "01000000001", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVICE_SECRETARY, 10L, null)));

        servantPrincipal = new UserPrincipal(3L, 1001L, "01001111111", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVANT, null, null)));
    }

    @Test
    void searchPeople_shortQuery_returnsEmpty() {
        List<SearchResultResponse> results = searchService.searchPeople("a", adminPrincipal);

        assertTrue(results.isEmpty());
        verify(staffPlacementRepository, never()).findAllActiveByAcademicYearId(any());
    }

    @Test
    void searchPeople_nullPrincipal_throwsUnauthorized() {
        AppException ex = assertThrows(AppException.class, () ->
                searchService.searchPeople("مارك", null));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void searchPeople_admin_returnsBothServantsAndStudents() {
        when(academicYearService.getCurrentEntity()).thenReturn(currentYear);
        when(staffPlacementRepository.findAllActiveByAcademicYearId(1L)).thenReturn(List.of(staffPlacement1, staffPlacement2));
        when(studentPlacementRepository.searchByAcademicYearIdAndStatus(1L, "مينا", StudentStatus.ACTIVE))
                .thenReturn(List.of(studentPlacement1)); // e.g. student search returned a match

        List<SearchResultResponse> results = searchService.searchPeople("مينا", adminPrincipal);

        assertEquals(2, results.size()); // 1 servant matching "مينا" + 1 student
        assertTrue(results.stream().anyMatch(r -> r.fullName().equals("مينا جرجس") && r.personType().equals("خادم")));
        assertTrue(results.stream().anyMatch(r -> r.fullName().equals("مارك سامح") && r.personType().equals("مخدوم")));
    }

    @Test
    void searchPeople_serviceSecretary_scopesToServiceOnly() {
        when(academicYearService.getCurrentEntity()).thenReturn(currentYear);
        // Staff placements include both ministry 1 and ministry 2
        when(staffPlacementRepository.findAllActiveByAcademicYearId(1L)).thenReturn(List.of(staffPlacement1, staffPlacement2));
        // Students include both
        when(studentPlacementRepository.searchByAcademicYearIdAndStatus(1L, "0100", StudentStatus.ACTIVE))
                .thenReturn(List.of(studentPlacement1, studentPlacement2));

        List<SearchResultResponse> results = searchService.searchPeople("0100", serviceSecretaryPrincipal);

        // Service secretary belongs to ministry1 (id: 10)
        // Should only see staffPlacement1 and studentPlacement1
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(r -> r.ministryId().equals(10L)));
        assertFalse(results.stream().anyMatch(r -> r.fullName().equals("بيتر ماجد"))); // in ministry 2
        assertFalse(results.stream().anyMatch(r -> r.fullName().equals("مارينا يوسف"))); // in ministry 2
    }

    @Test
    void searchPeople_servant_scopesToAssignedAndSelf() {
        when(academicYearService.getCurrentEntity()).thenReturn(currentYear);
        when(staffPlacementRepository.findAllActiveByAcademicYearId(1L)).thenReturn(List.of(staffPlacement1, staffPlacement2));
        when(studentPlacementRepository.searchByAcademicYearIdAndStatus(1L, "مينا", StudentStatus.ACTIVE))
                .thenReturn(List.of(studentPlacement1, studentPlacement2));

        List<SearchResultResponse> results = searchService.searchPeople("مينا", servantPrincipal);

        // Servant is 1001L
        // Sees servantPerson1 (self) and studentPlacement1 (assigned)
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(r -> r.personId().equals(1001L))); // self
        assertTrue(results.stream().anyMatch(r -> r.personId().equals(5001L))); // assigned student
        assertFalse(results.stream().anyMatch(r -> r.personId().equals(5002L))); // not assigned
    }
}
