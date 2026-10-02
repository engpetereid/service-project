package org.serviceproject.students.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.service.AcademicYearService;
import org.serviceproject.classes.entity.GradeClass;
import org.serviceproject.classes.repository.GradeClassRepository;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.RoleWithScope;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.ministries.entity.Ministry;
import org.serviceproject.ministries.repository.MinistryRepository;
import org.serviceproject.staff.entity.StaffPlacement;
import org.serviceproject.staff.repository.StaffPlacementRepository;
import org.serviceproject.students.dto.ChangeAssignmentRequest;
import org.serviceproject.students.dto.CreateStudentRequest;
import org.serviceproject.students.dto.StudentResponse;
import org.serviceproject.students.dto.UpdateStudentRequest;
import org.serviceproject.students.entity.StudentPlacement;
import org.serviceproject.students.entity.StudentStatus;
import org.serviceproject.students.repository.StudentPlacementRepository;
import org.serviceproject.users.entity.Gender;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.Role;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.repository.PersonRepository;
import org.serviceproject.users.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentPlacementRepository studentPlacementRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private MinistryRepository ministryRepository;

    @Mock
    private GradeClassRepository gradeClassRepository;

    @Mock
    private StaffPlacementRepository staffPlacementRepository;

    @Mock
    private AcademicYearService academicYearService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private StudentService studentService;

    private AcademicYear academicYear;
    private Ministry ministry1;
    private Ministry ministry2;
    private GradeClass class1;
    private GradeClass class2;
    private Person studentPerson1;
    private Person studentPerson2;
    private Person servantPerson1;
    private StudentPlacement placement1;
    private StudentPlacement placement2;

    private UserPrincipal adminPrincipal;
    private UserPrincipal serviceSecretaryPrincipal;
    private UserPrincipal classSecretaryPrincipal;
    private UserPrincipal servantPrincipal;

    @BeforeEach
    void setUp() {
        academicYear = new AcademicYear("2026/2027", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 8, 31), true);
        academicYear.setId(1L);

        ministry1 = new Ministry("ابتدائي");
        ministry1.setId(10L);

        ministry2 = new Ministry("إعدادي");
        ministry2.setId(20L);

        class1 = new GradeClass("أولى ابتدائي", ministry1);
        class1.setId(100L);

        class2 = new GradeClass("أولى إعدادي", ministry2);
        class2.setId(200L);

        studentPerson1 = new Person();
        studentPerson1.setId(5001L);
        studentPerson1.setFullName("مارك سامح");
        studentPerson1.setPhone("01111111111");
        studentPerson1.setGender(Gender.MALE);

        studentPerson2 = new Person();
        studentPerson2.setId(5002L);
        studentPerson2.setFullName("مارينا يوسف");
        studentPerson2.setPhone("01122222222");
        studentPerson2.setGender(Gender.FEMALE);

        servantPerson1 = new Person();
        servantPerson1.setId(3001L);
        servantPerson1.setFullName("مينا جرجس");
        servantPerson1.setPhone("01001111111");
        servantPerson1.setGender(Gender.MALE);

        placement1 = new StudentPlacement(studentPerson1, academicYear, ministry1, class1);
        placement1.setId(1L);
        placement1.assignServant(servantPerson1);

        placement2 = new StudentPlacement(studentPerson2, academicYear, ministry2, class2);
        placement2.setId(2L);

        adminPrincipal = new UserPrincipal(1L, 999L, "01000000000", "pass", true, 0,
                Set.of(new RoleWithScope(Role.GENERAL_ADMIN, null, null)));

        serviceSecretaryPrincipal = new UserPrincipal(2L, 888L, "01000000001", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVICE_SECRETARY, 10L, null)));

        classSecretaryPrincipal = new UserPrincipal(3L, 777L, "01000000002", "pass", true, 0,
                Set.of(new RoleWithScope(Role.CLASS_SECRETARY, null, 100L)));

        servantPrincipal = new UserPrincipal(4L, 3001L, "01001111111", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVANT, null, null)));
    }

    // ── findAll Scope Tests ──────────────────────────────────────────

    @Test
    void findAll_admin_returnsAll() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findAllByAcademicYearIdAndStatus(1L, StudentStatus.ACTIVE))
                .thenReturn(List.of(placement1, placement2));

        List<StudentResponse> result = studentService.findAll(adminPrincipal, null, null, null, null);

        assertEquals(2, result.size());
    }

    @Test
    void findAll_serviceSecretary_returnsScopedMinistry() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findAllByAcademicYearIdAndMinistryIdAndStatus(1L, 10L, StudentStatus.ACTIVE))
                .thenReturn(List.of(placement1));

        List<StudentResponse> result = studentService.findAll(serviceSecretaryPrincipal, null, null, null, null);

        assertEquals(1, result.size());
        assertEquals("ابتدائي", result.get(0).ministryName());
    }

    @Test
    void findAll_serviceSecretaryDifferentMinistry_throwsForbidden() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);

        AppException ex = assertThrows(AppException.class, () ->
                studentService.findAll(serviceSecretaryPrincipal, 20L, null, null, null));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void findAll_classSecretary_returnsScopedClass() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findAllByAcademicYearIdAndClassIdAndStatus(1L, 100L, StudentStatus.ACTIVE))
                .thenReturn(List.of(placement1));

        List<StudentResponse> result = studentService.findAll(classSecretaryPrincipal, null, null, null, null);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).classId());
    }

    @Test
    void findAll_servant_returnsOnlyAssigned() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findAllByAcademicYearIdAndServantIdAndStatus(1L, 3001L, StudentStatus.ACTIVE))
                .thenReturn(List.of(placement1));

        List<StudentResponse> result = studentService.findAll(servantPrincipal, null, null, null, null);

        assertEquals(1, result.size());
        assertEquals(3001L, result.get(0).servantId());
    }

    // ── findById Scope Tests ─────────────────────────────────────────

    @Test
    void findById_admin_success() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L))
                .thenReturn(Optional.of(placement1));

        StudentResponse response = studentService.findById(5001L, adminPrincipal);

        assertNotNull(response);
        assertEquals("مارك سامح", response.fullName());
    }

    @Test
    void findById_serviceSecretaryDifferentMinistry_throwsForbidden() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5002L, 1L))
                .thenReturn(Optional.of(placement2));

        AppException ex = assertThrows(AppException.class, () ->
                studentService.findById(5002L, serviceSecretaryPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void findById_servantUnassigned_throwsForbidden() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5002L, 1L))
                .thenReturn(Optional.of(placement2));

        AppException ex = assertThrows(AppException.class, () ->
                studentService.findById(5002L, servantPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    // ── create Tests ─────────────────────────────────────────────────

    @Test
    void create_admin_success() {
        CreateStudentRequest request = new CreateStudentRequest(
                "كيرلس نبيل", "01133333333", Gender.MALE, null, null, null,
                10L, 100L, null, "01144444444", "الرسم", "ملاحظات"
        );

        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(ministryRepository.findById(10L)).thenReturn(Optional.of(ministry1));
        when(gradeClassRepository.findByIdWithMinistry(100L)).thenReturn(Optional.of(class1));
        when(personRepository.findByPhone("01133333333")).thenReturn(Optional.empty());

        Person savedPerson = new Person();
        savedPerson.setId(5003L);
        savedPerson.setFullName("كيرلس نبيل");
        savedPerson.setPhone("01133333333");
        savedPerson.setGender(Gender.MALE);
        when(personRepository.save(any(Person.class))).thenReturn(savedPerson);

        StudentPlacement savedPlacement = new StudentPlacement(savedPerson, academicYear, ministry1, class1);
        savedPlacement.setId(3L);
        when(studentPlacementRepository.save(any(StudentPlacement.class))).thenReturn(savedPlacement);

        StudentResponse response = studentService.create(request, adminPrincipal);

        assertNotNull(response);
        assertEquals("كيرلس نبيل", response.fullName());
        verify(studentPlacementRepository).save(any(StudentPlacement.class));
    }

    @Test
    void create_servantInOwnClass_success() {
        CreateStudentRequest request = new CreateStudentRequest(
                "كيرلس نبيل", "01133333333", Gender.MALE, null, null, null,
                10L, 100L, null, null, null, null
        );

        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        // Servant placement in class1
        StaffPlacement staffPlacement = new StaffPlacement(servantPerson1, academicYear, ministry1, class1);
        when(staffPlacementRepository.findByPersonIdAndAcademicYearId(3001L, 1L)).thenReturn(Optional.of(staffPlacement));

        when(ministryRepository.findById(10L)).thenReturn(Optional.of(ministry1));
        when(gradeClassRepository.findByIdWithMinistry(100L)).thenReturn(Optional.of(class1));
        when(personRepository.findByPhone("01133333333")).thenReturn(Optional.empty());

        Person savedPerson = new Person();
        savedPerson.setId(5003L);
        savedPerson.setFullName("كيرلس نبيل");
        savedPerson.setPhone("01133333333");
        savedPerson.setGender(Gender.MALE);
        when(personRepository.save(any(Person.class))).thenReturn(savedPerson);

        StudentPlacement savedPlacement = new StudentPlacement(savedPerson, academicYear, ministry1, class1);
        savedPlacement.setId(3L);
        when(studentPlacementRepository.save(any(StudentPlacement.class))).thenReturn(savedPlacement);

        StudentResponse response = studentService.create(request, servantPrincipal);

        assertNotNull(response);
        assertEquals("كيرلس نبيل", response.fullName());
    }

    @Test
    void create_deletedStudent_restoresAndReusesPlacement_success() {
        CreateStudentRequest request = new CreateStudentRequest(
                "مرقس عادل", "01188888888", Gender.MALE, null, null, null,
                10L, 100L, null, null, null, null
        );

        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(ministryRepository.findById(10L)).thenReturn(Optional.of(ministry1));
        when(gradeClassRepository.findByIdWithMinistry(100L)).thenReturn(Optional.of(class1));

        Person deletedPerson = new Person();
        deletedPerson.setId(7001L);
        deletedPerson.setFullName("مرقس عادل القديم");
        deletedPerson.setPhone("01188888888");
        deletedPerson.setGender(Gender.MALE);
        deletedPerson.setDeletedAt(java.time.LocalDateTime.now().minusDays(10));

        when(personRepository.findByPhone("01188888888")).thenReturn(Optional.of(deletedPerson));
        when(personRepository.save(any(Person.class))).thenAnswer(i -> i.getArgument(0));

        StudentPlacement existingPlacement = new StudentPlacement(deletedPerson, academicYear, ministry1, class1);
        existingPlacement.setId(77L);
        when(studentPlacementRepository.findAnyByPersonIdAndAcademicYearId(7001L, 1L))
                .thenReturn(Optional.of(existingPlacement));
        when(studentPlacementRepository.save(any(StudentPlacement.class))).thenAnswer(i -> i.getArgument(0));

        StudentResponse response = studentService.create(request, adminPrincipal);

        assertNotNull(response);
        assertFalse(deletedPerson.isDeleted());
        assertEquals("مرقس عادل", deletedPerson.getFullName());
        verify(studentPlacementRepository).save(existingPlacement);
    }

    @Test
    void create_servantInOtherClass_throwsForbidden() {
        CreateStudentRequest request = new CreateStudentRequest(
                "كيرلس نبيل", "01133333333", Gender.MALE, null, null, null,
                20L, 200L, null, null, null, null
        );

        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        // Servant placement in class1 (ministry 10, class 100)
        StaffPlacement staffPlacement = new StaffPlacement(servantPerson1, academicYear, ministry1, class1);
        when(staffPlacementRepository.findByPersonIdAndAcademicYearId(3001L, 1L)).thenReturn(Optional.of(staffPlacement));

        AppException ex = assertThrows(AppException.class, () ->
                studentService.create(request, servantPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    // ── update Tests ─────────────────────────────────────────────────

    @Test
    void update_adminCanChangeClass_success() {
        UpdateStudentRequest request = new UpdateStudentRequest(
                "مارك سامح بعد النقل", "01111111111", Gender.MALE, null, null, null,
                20L, 200L, null, null, null
        );

        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L)).thenReturn(Optional.of(placement1));
        when(ministryRepository.findById(20L)).thenReturn(Optional.of(ministry2));
        when(gradeClassRepository.findByIdWithMinistry(200L)).thenReturn(Optional.of(class2));
        when(studentPlacementRepository.save(any(StudentPlacement.class))).thenReturn(placement1);

        StudentResponse response = studentService.update(5001L, request, adminPrincipal);

        assertNotNull(response);
        assertEquals(20L, placement1.getMinistry().getId());
        assertEquals(200L, placement1.getGradeClass().getId());
    }

    @Test
    void update_secretaryCannotChangeClass_throwsForbidden() {
        UpdateStudentRequest request = new UpdateStudentRequest(
                "مارك سامح", "01111111111", Gender.MALE, null, null, null,
                10L, 200L, null, null, null
        );

        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L)).thenReturn(Optional.of(placement1));

        AppException ex = assertThrows(AppException.class, () ->
                studentService.update(5001L, request, serviceSecretaryPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("CANNOT_CHANGE_SERVICE_OR_CLASS", ex.getCode());
    }

    @Test
    void update_servantCannotChangeClass_throwsForbidden() {
        UpdateStudentRequest request = new UpdateStudentRequest(
                "مارك سامح", "01111111111", Gender.MALE, null, null, null,
                10L, 200L, null, null, null
        );

        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L)).thenReturn(Optional.of(placement1));

        AppException ex = assertThrows(AppException.class, () ->
                studentService.update(5001L, request, servantPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("CANNOT_CHANGE_SERVICE_OR_CLASS", ex.getCode());
    }

    @Test
    void update_servantCannotEditOtherServantsStudent_throwsForbidden() {
        UserPrincipal otherServantPrincipal = new UserPrincipal(2L, 3002L, "01002222222", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVANT, 10L, 100L)));

        UpdateStudentRequest request = new UpdateStudentRequest(
                "مارك سامح المعدل", "01111111111", Gender.MALE, null, null, null,
                null, null, null, null, null
        );

        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L)).thenReturn(Optional.of(placement1));

        AppException ex = assertThrows(AppException.class, () ->
                studentService.update(5001L, request, otherServantPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    // ── changeAssignment Tests ───────────────────────────────────────

    @Test
    void changeAssignment_secretary_success() {
        ChangeAssignmentRequest request = new ChangeAssignmentRequest(3001L);

        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L)).thenReturn(Optional.of(placement1));
        when(personRepository.findByIdAndDeletedAtIsNull(3001L)).thenReturn(Optional.of(servantPerson1));

        StaffPlacement staffPlacement = new StaffPlacement(servantPerson1, academicYear, ministry1, class1);
        when(staffPlacementRepository.findByPersonIdAndAcademicYearId(3001L, 1L)).thenReturn(Optional.of(staffPlacement));
        when(studentPlacementRepository.save(any(StudentPlacement.class))).thenReturn(placement1);

        StudentResponse response = studentService.changeAssignment(5001L, request, classSecretaryPrincipal);

        assertNotNull(response);
        assertEquals(3001L, placement1.getResponsibleServant().getId());
    }

    @Test
    void changeAssignment_servantRole_throwsForbidden() {
        ChangeAssignmentRequest request = new ChangeAssignmentRequest(3001L);

        AppException ex = assertThrows(AppException.class, () ->
                studentService.changeAssignment(5001L, request, servantPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void changeAssignment_servantDifferentClass_throwsBadRequest() {
        ChangeAssignmentRequest request = new ChangeAssignmentRequest(3001L);

        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L)).thenReturn(Optional.of(placement1));
        when(personRepository.findByIdAndDeletedAtIsNull(3001L)).thenReturn(Optional.of(servantPerson1));

        // Staff placement is in class2 instead of class1
        StaffPlacement staffPlacement = new StaffPlacement(servantPerson1, academicYear, ministry2, class2);
        when(staffPlacementRepository.findByPersonIdAndAcademicYearId(3001L, 1L)).thenReturn(Optional.of(staffPlacement));

        AppException ex = assertThrows(AppException.class, () ->
                studentService.changeAssignment(5001L, request, adminPrincipal));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("SERVANT_CLASS_MISMATCH", ex.getCode());
    }

    @Test
    void changeAssignment_classSecretaryAssignsSelf_autoPlacesAndSucceeds() {
        Person secPerson = new Person();
        secPerson.setId(3003L);
        secPerson.setFullName("أمين الفصل");
        secPerson.setPhone("01033333333");
        secPerson.setGender(Gender.MALE);

        UserAccount secAccount = new UserAccount();
        secAccount.setPerson(secPerson);

        ChangeAssignmentRequest request = new ChangeAssignmentRequest(3003L);

        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L)).thenReturn(Optional.of(placement1));
        when(personRepository.findByIdAndDeletedAtIsNull(3003L)).thenReturn(Optional.of(secPerson));

        // No prior staff placement in class1
        when(staffPlacementRepository.findByPersonIdAndAcademicYearId(3003L, 1L)).thenReturn(Optional.empty());
        when(userAccountRepository.findClassSecretariesByClassId(100L)).thenReturn(List.of(secAccount));
        when(gradeClassRepository.findByIdWithMinistry(100L)).thenReturn(Optional.of(class1));
        when(studentPlacementRepository.save(any(StudentPlacement.class))).thenReturn(placement1);

        StudentResponse response = studentService.changeAssignment(5001L, request, classSecretaryPrincipal);

        assertNotNull(response);
        assertEquals(3003L, placement1.getResponsibleServant().getId());
        verify(staffPlacementRepository).save(any(StaffPlacement.class));
    }

    @Test
    void findAll_classSecretary_withSearch_filtersByName() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findAllByAcademicYearIdAndClassIdAndStatus(1L, 100L, StudentStatus.ACTIVE))
                .thenReturn(List.of(placement1, placement2));

        List<StudentResponse> result = studentService.findAll(classSecretaryPrincipal, null, null, null, null, "مارك");

        assertEquals(1, result.size());
        assertEquals("مارك سامح", result.get(0).fullName());
    }

    // ── softDelete & restore Tests ───────────────────────────────────

    @Test
    void softDelete_servantRole_throwsForbidden() {
        AppException ex = assertThrows(AppException.class, () ->
                studentService.softDelete(5001L, servantPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void softDelete_admin_success() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L)).thenReturn(Optional.of(placement1));

        studentService.softDelete(5001L, adminPrincipal);

        assertTrue(studentPerson1.isDeleted());
        verify(personRepository).save(studentPerson1);
    }

    @Test
    void restore_success() {
        studentPerson1.softDelete();
        when(personRepository.findById(5001L)).thenReturn(Optional.of(studentPerson1));

        studentService.restore(5001L);

        assertFalse(studentPerson1.isDeleted());
        verify(personRepository).save(studentPerson1);
    }

    @Test
    void testSerialization() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
        StudentResponse res = new StudentResponse(
                1L, "كيرلس", "0123", "MALE", null, null, null,
                10L, "ابتدائي", 20L, "فصل أ", 30L, "خادم أ",
                1L, "2024", "ACTIVE", null, null, null, null, true
        );
        String json = mapper.writeValueAsString(res);
        assertTrue(json.contains("\"responsibleServantId\":30"));
        assertTrue(json.contains("\"responsibleServantName\":\"خادم أ\""));
        assertTrue(json.contains("\"id\":1"));
    }
}
