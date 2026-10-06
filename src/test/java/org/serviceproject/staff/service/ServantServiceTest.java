package org.serviceproject.staff.service;

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
import org.serviceproject.staff.dto.CreateServantRequest;
import org.serviceproject.staff.dto.ServantResponse;
import org.serviceproject.staff.dto.UpdateServantRequest;
import org.serviceproject.staff.entity.StaffPlacement;
import org.serviceproject.staff.repository.StaffPlacementRepository;
import org.serviceproject.users.entity.Gender;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.Role;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.repository.PersonRepository;
import org.serviceproject.users.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServantServiceTest {

    @Mock
    private StaffPlacementRepository staffPlacementRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private MinistryRepository ministryRepository;

    @Mock
    private GradeClassRepository gradeClassRepository;

    @Mock
    private AcademicYearService academicYearService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private org.serviceproject.students.repository.StudentPlacementRepository studentPlacementRepository;

    @Mock
    private org.serviceproject.notifications.service.NotificationService notificationService;

    @InjectMocks
    private ServantService servantService;

    private AcademicYear academicYear;
    private Ministry ministry1;
    private Ministry ministry2;
    private GradeClass class1;
    private GradeClass class2;
    private Person servantPerson1;
    private Person servantPerson2;
    private StaffPlacement placement1;
    private StaffPlacement placement2;

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

        servantPerson1 = new Person();
        servantPerson1.setId(1000L);
        servantPerson1.setFullName("مينا جرجس");
        servantPerson1.setPhone("01001111111");
        servantPerson1.setGender(Gender.MALE);

        servantPerson2 = new Person();
        servantPerson2.setId(2000L);
        servantPerson2.setFullName("بيشوي سمير");
        servantPerson2.setPhone("01002222222");
        servantPerson2.setGender(Gender.MALE);

        placement1 = new StaffPlacement(servantPerson1, academicYear, ministry1, class1);
        placement1.setId(1L);

        placement2 = new StaffPlacement(servantPerson2, academicYear, ministry2, class2);
        placement2.setId(2L);

        // Principals
        adminPrincipal = new UserPrincipal(1L, 999L, "01000000000", "pass", true, 0,
                Set.of(new RoleWithScope(Role.GENERAL_ADMIN, null, null)));

        serviceSecretaryPrincipal = new UserPrincipal(2L, 888L, "01000000001", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVICE_SECRETARY, 10L, null)));

        classSecretaryPrincipal = new UserPrincipal(3L, 777L, "01000000002", "pass", true, 0,
                Set.of(new RoleWithScope(Role.CLASS_SECRETARY, null, 100L)));

        servantPrincipal = new UserPrincipal(4L, 1000L, "01001111111", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVANT, null, null)));
    }

    // ── findAll Scope Tests ──────────────────────────────────────────

    @Test
    void findAll_admin_returnsAll() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(staffPlacementRepository.findAllActiveByAcademicYearId(1L)).thenReturn(List.of(placement1, placement2));

        List<ServantResponse> result = servantService.findAll(adminPrincipal, null, null);

        assertEquals(2, result.size());
    }

    @Test
    void findAll_adminWithMinistryFilter_returnsFiltered() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(staffPlacementRepository.findAllActiveByAcademicYearIdAndMinistryId(1L, 10L)).thenReturn(List.of(placement1));

        List<ServantResponse> result = servantService.findAll(adminPrincipal, 10L, null);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).ministryId());
    }

    @Test
    void findAll_serviceSecretary_returnsScopedMinistry() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(staffPlacementRepository.findAllActiveByAcademicYearIdAndMinistryId(1L, 10L)).thenReturn(List.of(placement1));

        List<ServantResponse> result = servantService.findAll(serviceSecretaryPrincipal, null, null);

        assertEquals(1, result.size());
        assertEquals("ابتدائي", result.get(0).ministryName());
    }

    @Test
    void findAll_serviceSecretaryDifferentMinistry_throwsForbidden() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);

        AppException ex = assertThrows(AppException.class, () ->
                servantService.findAll(serviceSecretaryPrincipal, 20L, null));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void findAll_classSecretary_returnsScopedClass() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(staffPlacementRepository.findAllActiveByAcademicYearIdAndClassId(1L, 100L)).thenReturn(List.of(placement1));

        List<ServantResponse> result = servantService.findAll(classSecretaryPrincipal, null, null);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).classId());
    }

    @Test
    void findAll_classSecretary_autoIncludesSecretaryIfNotPlaced() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(staffPlacementRepository.findAllActiveByAcademicYearIdAndClassId(1L, 100L)).thenReturn(List.of());

        Person secPerson = new Person();
        secPerson.setId(999L);
        secPerson.setFullName("أمين الفصل");
        secPerson.setPhone("01099999999");
        secPerson.setGender(Gender.MALE);

        UserAccount secAccount = new UserAccount();
        secAccount.setPerson(secPerson);
        secAccount.setEnabled(true);
        org.serviceproject.users.entity.UserRole role = new org.serviceproject.users.entity.UserRole(Role.CLASS_SECRETARY);
        role.setClassId(100L);
        secAccount.addRole(role);

        when(userAccountRepository.findClassSecretariesByClassId(100L)).thenReturn(List.of(secAccount));
        when(userAccountRepository.findByPersonId(999L)).thenReturn(Optional.of(secAccount));
        when(gradeClassRepository.findByIdWithMinistry(100L)).thenReturn(Optional.of(class1));

        List<ServantResponse> result = servantService.findAll(classSecretaryPrincipal, null, null);

        assertEquals(1, result.size());
        assertEquals(999L, result.get(0).personId());
        assertTrue(result.get(0).isClassSecretary());
        verify(staffPlacementRepository, never()).save(any(StaffPlacement.class));
    }

    @Test
    void findAll_servant_returnsOnlySelf() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(staffPlacementRepository.findByPersonIdAndAcademicYearId(1000L, 1L)).thenReturn(Optional.of(placement1));

        List<ServantResponse> result = servantService.findAll(servantPrincipal, null, null);

        assertEquals(1, result.size());
        assertEquals(1000L, result.get(0).personId());
    }

    // ── findById Scope Tests ─────────────────────────────────────────

    @Test
    void findById_admin_success() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(staffPlacementRepository.findByPersonIdAndAcademicYearId(1000L, 1L)).thenReturn(Optional.of(placement1));

        ServantResponse response = servantService.findById(1000L, adminPrincipal);

        assertNotNull(response);
        assertEquals("مينا جرجس", response.fullName());
    }

    @Test
    void findById_serviceSecretaryDifferentMinistry_throwsForbidden() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(staffPlacementRepository.findByPersonIdAndAcademicYearId(2000L, 1L)).thenReturn(Optional.of(placement2));

        AppException ex = assertThrows(AppException.class, () ->
                servantService.findById(2000L, serviceSecretaryPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void findById_servantOtherPerson_throwsForbidden() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(staffPlacementRepository.findByPersonIdAndAcademicYearId(2000L, 1L)).thenReturn(Optional.of(placement2));

        AppException ex = assertThrows(AppException.class, () ->
                servantService.findById(2000L, servantPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    // ── create Tests ─────────────────────────────────────────────────

    @Test
    void create_admin_success() {
        CreateServantRequest request = new CreateServantRequest(
                "يوحنا سامي", "01003333333", Gender.MALE, null, null, null, 10L, 100L, "secret"
        );

        when(ministryRepository.findById(10L)).thenReturn(Optional.of(ministry1));
        when(gradeClassRepository.findByIdWithMinistry(100L)).thenReturn(Optional.of(class1));
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(personRepository.findByPhone("01003333333")).thenReturn(Optional.empty());

        Person savedPerson = new Person();
        savedPerson.setId(3000L);
        savedPerson.setFullName("يوحنا سامي");
        savedPerson.setPhone("01003333333");
        savedPerson.setGender(Gender.MALE);
        when(personRepository.save(any(Person.class))).thenReturn(savedPerson);

        StaffPlacement savedPlacement = new StaffPlacement(savedPerson, academicYear, ministry1, class1);
        savedPlacement.setId(3L);
        when(staffPlacementRepository.save(any(StaffPlacement.class))).thenReturn(savedPlacement);
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        ServantResponse response = servantService.create(request, adminPrincipal);

        assertNotNull(response);
        assertEquals("يوحنا سامي", response.fullName());
        verify(staffPlacementRepository).save(any(StaffPlacement.class));
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    @Test
    void create_withoutPassword_createsAndEnablesAccountAutomatically() {
        CreateServantRequest request = new CreateServantRequest(
                "أرسانيوس عاطف", "01004444444", Gender.MALE, null, null, null, 10L, 100L, null
        );

        when(ministryRepository.findById(10L)).thenReturn(Optional.of(ministry1));
        when(gradeClassRepository.findByIdWithMinistry(100L)).thenReturn(Optional.of(class1));
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(personRepository.findByPhone("01004444444")).thenReturn(Optional.empty());

        Person savedPerson = new Person();
        savedPerson.setId(3005L);
        savedPerson.setFullName("أرسانيوس عاطف");
        savedPerson.setPhone("01004444444");
        savedPerson.setGender(Gender.MALE);
        when(personRepository.save(any(Person.class))).thenReturn(savedPerson);

        StaffPlacement savedPlacement = new StaffPlacement(savedPerson, academicYear, ministry1, class1);
        savedPlacement.setId(5L);
        when(staffPlacementRepository.save(any(StaffPlacement.class))).thenReturn(savedPlacement);
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        ServantResponse response = servantService.create(request, adminPrincipal);

        assertNotNull(response);
        assertEquals("أرسانيوس عاطف", response.fullName());
        verify(staffPlacementRepository).save(any(StaffPlacement.class));
        verify(userAccountRepository).save(argThat(acc -> acc.isEnabled() && acc.hasRole(Role.SERVANT)));
    }

    @Test
    void create_deletedServant_restoresAndReusesPlacement_success() {
        CreateServantRequest request = new CreateServantRequest(
                "مينا شنودة", "01005555555", Gender.MALE, null, null, null, 10L, 100L, null
        );

        when(ministryRepository.findById(10L)).thenReturn(Optional.of(ministry1));
        when(gradeClassRepository.findByIdWithMinistry(100L)).thenReturn(Optional.of(class1));
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);

        Person deletedPerson = new Person();
        deletedPerson.setId(9001L);
        deletedPerson.setFullName("مينا شنودة القديم");
        deletedPerson.setPhone("01005555555");
        deletedPerson.setGender(Gender.MALE);
        deletedPerson.setDeletedAt(java.time.LocalDateTime.now().minusDays(5));

        when(personRepository.findByPhone("01005555555")).thenReturn(Optional.of(deletedPerson));
        when(personRepository.save(any(Person.class))).thenAnswer(i -> i.getArgument(0));

        StaffPlacement existingPlacement = new StaffPlacement(deletedPerson, academicYear, ministry1, class1);
        existingPlacement.setId(99L);
        when(staffPlacementRepository.findAnyByPersonIdAndAcademicYearId(9001L, 1L))
                .thenReturn(Optional.of(existingPlacement));
        when(staffPlacementRepository.save(any(StaffPlacement.class))).thenAnswer(i -> i.getArgument(0));

        UserAccount existingAcc = new UserAccount();
        existingAcc.setId(901L);
        existingAcc.setPerson(deletedPerson);
        existingAcc.setEnabled(false);
        when(userAccountRepository.findByPersonId(9001L)).thenReturn(Optional.of(existingAcc));

        ServantResponse response = servantService.create(request, adminPrincipal);

        assertNotNull(response);
        assertFalse(deletedPerson.isDeleted());
        assertTrue(existingAcc.isEnabled());
        verify(staffPlacementRepository).save(existingPlacement);
        verify(userAccountRepository).save(existingAcc);
    }

    @Test
    void create_servantRoleOnly_throwsForbidden() {
        CreateServantRequest request = new CreateServantRequest(
                "يوحنا سامي", "01003333333", Gender.MALE, null, null, null, 10L, 100L, null
        );

        AppException ex = assertThrows(AppException.class, () ->
                servantService.create(request, servantPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void create_classMinistryMismatch_throwsBadRequest() {
        CreateServantRequest request = new CreateServantRequest(
                "يوحنا سامي", "01003333333", Gender.MALE, null, null, null, 10L, 200L, null
        );

        when(ministryRepository.findById(10L)).thenReturn(Optional.of(ministry1));
        when(gradeClassRepository.findByIdWithMinistry(200L)).thenReturn(Optional.of(class2));

        AppException ex = assertThrows(AppException.class, () ->
                servantService.create(request, adminPrincipal));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("CLASS_MINISTRY_MISMATCH", ex.getCode());
    }

    @Test
    void create_serviceSecretaryOutsideMinistry_throwsForbidden() {
        CreateServantRequest request = new CreateServantRequest(
                "يوحنا سامي", "01003333333", Gender.MALE, null, null, null, 20L, 200L, null
        );

        AppException ex = assertThrows(AppException.class, () ->
                servantService.create(request, serviceSecretaryPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    // ── update Tests ─────────────────────────────────────────────────

    @Test
    void update_success() {
        UpdateServantRequest request = new UpdateServantRequest(
                "مينا جرجس بعد التعديل", "01001111111", Gender.MALE, null, null, null, 10L, 100L
        );

        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(staffPlacementRepository.findByPersonIdAndAcademicYearId(1000L, 1L)).thenReturn(Optional.of(placement1));
        when(ministryRepository.findById(10L)).thenReturn(Optional.of(ministry1));
        when(gradeClassRepository.findByIdWithMinistry(100L)).thenReturn(Optional.of(class1));
        when(staffPlacementRepository.save(any(StaffPlacement.class))).thenReturn(placement1);

        ServantResponse response = servantService.update(1000L, request, adminPrincipal);

        assertNotNull(response);
        assertEquals("مينا جرجس بعد التعديل", servantPerson1.getFullName());
    }

    // ── softDelete & restore Tests ───────────────────────────────────

    @Test
    void softDelete_admin_success() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(staffPlacementRepository.findByPersonIdAndAcademicYearId(1000L, 1L)).thenReturn(Optional.of(placement1));

        UserAccount acc = new UserAccount();
        acc.setEnabled(true);
        when(userAccountRepository.findByPersonId(1000L)).thenReturn(Optional.of(acc));

        servantService.softDelete(1000L, adminPrincipal);

        assertTrue(servantPerson1.isDeleted());
        assertFalse(acc.isEnabled());
        verify(personRepository).save(servantPerson1);
        verify(userAccountRepository).save(acc);
    }

    @Test
    void softDelete_unassignsActiveStudents_andNotifiesSecretary() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(staffPlacementRepository.findByPersonIdAndAcademicYearId(1000L, 1L)).thenReturn(Optional.of(placement1));

        UserAccount acc = new UserAccount();
        acc.setEnabled(true);
        when(userAccountRepository.findByPersonId(1000L)).thenReturn(Optional.of(acc));

        Person student = new Person();
        student.setId(5001L);
        student.setFullName("مارك سامح");
        student.setGender(Gender.MALE);

        org.serviceproject.students.entity.StudentPlacement studentPlacement =
                new org.serviceproject.students.entity.StudentPlacement(student, academicYear, ministry1, class1);
        studentPlacement.assignServant(servantPerson1);

        when(studentPlacementRepository.findAllByAcademicYearIdAndServantIdAndStatus(1L, 1000L, org.serviceproject.students.entity.StudentStatus.ACTIVE))
                .thenReturn(List.of(studentPlacement));

        UserAccount secretaryAcc = new UserAccount();
        secretaryAcc.setId(201L);
        when(userAccountRepository.findClassSecretariesByClassId(100L)).thenReturn(List.of(secretaryAcc));

        servantService.softDelete(1000L, adminPrincipal);

        assertNull(studentPlacement.getResponsibleServant());
        verify(studentPlacementRepository).save(studentPlacement);
        verify(notificationService).createNotification(eq(secretaryAcc), anyString(), anyString(), eq(org.serviceproject.notifications.entity.NotificationType.SYSTEM), anyString());
    }

    @Test
    void restore_success() {
        servantPerson1.softDelete();
        when(personRepository.findById(1000L)).thenReturn(Optional.of(servantPerson1));

        UserAccount acc = new UserAccount();
        acc.setEnabled(false);
        when(userAccountRepository.findByPersonId(1000L)).thenReturn(Optional.of(acc));

        servantService.restore(1000L);

        assertFalse(servantPerson1.isDeleted());
        assertTrue(acc.isEnabled());
        verify(personRepository).save(servantPerson1);
        verify(userAccountRepository).save(acc);
    }

    // ── createOrEnableAccount & Reset Password Tests ─────────────────

    @Test
    void createOrEnableAccount_existingAccount_resetsPassword() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(staffPlacementRepository.findByPersonIdAndAcademicYearId(1000L, 1L)).thenReturn(Optional.of(placement1));

        UserAccount acc = new UserAccount();
        acc.setPerson(servantPerson1);
        acc.setPassword("old-hash");
        acc.setEnabled(true);
        when(userAccountRepository.findByPersonId(1000L)).thenReturn(Optional.of(acc));
        when(passwordEncoder.encode("NewSecret123")).thenReturn("encoded-secret");

        ServantResponse res = servantService.createOrEnableAccount(1000L, "NewSecret123", adminPrincipal);

        assertNotNull(res);
        assertEquals("encoded-secret", acc.getPassword());
        assertTrue(acc.getTokenVersion() > 0);
        verify(userAccountRepository).save(acc);
    }

    @Test
    void createOrEnableAccount_byServiceSecretary_withinScope_success() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(staffPlacementRepository.findByPersonIdAndAcademicYearId(1000L, 1L)).thenReturn(Optional.of(placement1));

        UserAccount acc = new UserAccount();
        acc.setPerson(servantPerson1);
        acc.setEnabled(true);
        when(userAccountRepository.findByPersonId(1000L)).thenReturn(Optional.of(acc));
        when(passwordEncoder.encode("SecretPass99")).thenReturn("encoded-99");

        ServantResponse res = servantService.createOrEnableAccount(1000L, "SecretPass99", serviceSecretaryPrincipal);

        assertNotNull(res);
        assertEquals("encoded-99", acc.getPassword());
        verify(userAccountRepository).save(acc);
    }

    @Test
    void createOrEnableAccount_passwordCannotBePhone() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(staffPlacementRepository.findByPersonIdAndAcademicYearId(1000L, 1L)).thenReturn(Optional.of(placement1));

        UserAccount acc = new UserAccount();
        acc.setPerson(servantPerson1);
        acc.setEnabled(true);
        when(userAccountRepository.findByPersonId(1000L)).thenReturn(Optional.of(acc));

        AppException ex = assertThrows(AppException.class, () ->
                servantService.createOrEnableAccount(1000L, servantPerson1.getPhone(), adminPrincipal)
        );
        assertEquals("PASSWORD_CANNOT_BE_PHONE", ex.getCode());
    }
}
