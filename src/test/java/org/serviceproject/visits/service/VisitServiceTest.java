package org.serviceproject.visits.service;

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
import org.serviceproject.settings.entity.SystemSetting;
import org.serviceproject.settings.repository.SystemSettingRepository;
import org.serviceproject.students.entity.StudentPlacement;
import org.serviceproject.students.entity.StudentStatus;
import org.serviceproject.students.repository.StudentPlacementRepository;
import org.serviceproject.users.entity.Gender;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.Role;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.repository.UserAccountRepository;
import org.serviceproject.visits.dto.ServantCurrentWeekResponse;
import org.serviceproject.visits.dto.UpdateVisitRequest;
import org.serviceproject.visits.dto.VisitRequest;
import org.serviceproject.visits.dto.VisitResponse;
import org.serviceproject.visits.entity.VisitMethod;
import org.serviceproject.visits.entity.VisitRecord;
import org.serviceproject.visits.repository.VisitRecordRepository;
import org.serviceproject.weeks.dto.WeekResponse;
import org.serviceproject.weeks.entity.Week;
import org.serviceproject.weeks.service.WeekService;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitServiceTest {

    @Mock
    private VisitRecordRepository visitRecordRepository;

    @Mock
    private StudentPlacementRepository studentPlacementRepository;

    @Mock
    private WeekService weekService;

    @Mock
    private AcademicYearService academicYearService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @InjectMocks
    private VisitService visitService;

    private AcademicYear academicYear;
    private Week week;
    private Ministry ministry;
    private GradeClass gradeClass;
    private Person studentPerson;
    private Person servantPerson;
    private UserAccount servantAccount;
    private StudentPlacement studentPlacement;
    private VisitRecord visitRecord;

    private UserPrincipal adminPrincipal;
    private UserPrincipal servantPrincipal;
    private UserPrincipal otherServantPrincipal;

    @BeforeEach
    void setUp() {
        academicYear = new AcademicYear("2026/2027", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 8, 31), true);
        academicYear.setId(1L);

        week = new Week(LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 10));
        week.setId(10L);

        ministry = new Ministry("ابتدائي");
        ministry.setId(100L);

        gradeClass = new GradeClass("أولى ابتدائي", ministry);
        gradeClass.setId(1000L);

        studentPerson = new Person();
        studentPerson.setId(5001L);
        studentPerson.setFullName("كيرلس مينا");
        studentPerson.setPhone("01111111111");
        studentPerson.setGender(Gender.MALE);

        servantPerson = new Person();
        servantPerson.setId(3001L);
        servantPerson.setFullName("مينا جرجس");
        servantPerson.setPhone("01001111111");
        servantPerson.setGender(Gender.MALE);

        servantAccount = new UserAccount();
        servantAccount.setId(201L);
        servantAccount.setPerson(servantPerson);

        studentPlacement = new StudentPlacement(studentPerson, academicYear, ministry, gradeClass);
        studentPlacement.assignServant(servantPerson);

        visitRecord = new VisitRecord(
                studentPerson, week, academicYear,
                ministry, gradeClass, servantPerson,
                VisitMethod.VISIT, 5, 4, 14, "ملاحظات", servantAccount
        );
        visitRecord.setId(501L);

        adminPrincipal = new UserPrincipal(101L, 999L, "01000000000", "pass", true, 0,
                Set.of(new RoleWithScope(Role.GENERAL_ADMIN, null, null)));

        servantPrincipal = new UserPrincipal(201L, 3001L, "01001111111", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVANT, null, null)));

        otherServantPrincipal = new UserPrincipal(301L, 3002L, "01002222222", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVANT, null, null)));
    }

    @Test
    void getCurrentWeekWorkflow_servant_returnsAssignedStudentsWithStatus() {
        WeekResponse weekResponse = new WeekResponse(10L, LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 10), false, true);

        when(weekService.getCurrentWeek()).thenReturn(weekResponse);
        when(weekService.getCurrentWeekEntity()).thenReturn(week);
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);

        when(studentPlacementRepository.findAllByAcademicYearIdAndServantIdAndStatus(1L, 3001L, StudentStatus.ACTIVE))
                .thenReturn(List.of(studentPlacement));

        when(visitRecordRepository.findByStudentIdAndWeekId(5001L, 10L))
                .thenReturn(Optional.of(visitRecord));

        ServantCurrentWeekResponse response = visitService.getCurrentWeekWorkflow(servantPrincipal);

        assertNotNull(response);
        assertEquals(10L, response.week().id());
        assertEquals(1, response.students().size());
        assertEquals("كيرلس مينا", response.students().get(0).fullName());
        assertNotNull(response.students().get(0).visit());
        assertEquals("VISIT", response.students().get(0).visit().method());
        assertEquals(5, response.students().get(0).visit().prayerScore());
    }

    @Test
    void create_success_capturesSnapshots() {
        VisitRequest request = new VisitRequest(
                5001L, 10L, VisitMethod.VISIT, 6, 7, 18, "زيارة ممتازة"
        );

        when(weekService.getWeekOrThrow(10L)).thenReturn(week);
        when(weekService.isWeekLockedForUser(week, servantPrincipal)).thenReturn(false);
        when(visitRecordRepository.existsByStudentIdAndWeekId(5001L, 10L)).thenReturn(false);
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L)).thenReturn(Optional.of(studentPlacement));
        when(systemSettingRepository.findBySettingKey("MAX_NOTE_SCORE"))
                .thenReturn(Optional.of(new SystemSetting("MAX_NOTE_SCORE", "21")));
        when(userAccountRepository.findById(201L)).thenReturn(Optional.of(servantAccount));

        when(visitRecordRepository.save(any(VisitRecord.class))).thenAnswer(i -> {
            VisitRecord vr = i.getArgument(0);
            vr.setId(601L);
            return vr;
        });

        VisitResponse response = visitService.create(request, servantPrincipal);

        assertNotNull(response);
        assertEquals(5001L, response.studentId());
        assertEquals(100L, response.ministrySnapId());
        assertEquals(1000L, response.classSnapId());
        assertEquals(3001L, response.servantSnapId());
        assertEquals("VISIT", response.method());
        assertEquals(6, response.prayerScore());

        verify(visitRecordRepository).save(any(VisitRecord.class));
    }

    @Test
    void create_weekLocked_throwsForbidden() {
        VisitRequest request = new VisitRequest(5001L, 10L, VisitMethod.VISIT, 6, 7, 18, null);

        when(weekService.getWeekOrThrow(10L)).thenReturn(week);
        when(weekService.isWeekLockedForUser(week, servantPrincipal)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> visitService.create(request, servantPrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("WEEK_LOCKED", ex.getCode());
    }

    @Test
    void create_duplicateVisit_throwsConflict() {
        VisitRequest request = new VisitRequest(5001L, 10L, VisitMethod.VISIT, 6, 7, 18, null);

        when(weekService.getWeekOrThrow(10L)).thenReturn(week);
        when(weekService.isWeekLockedForUser(week, servantPrincipal)).thenReturn(false);
        when(visitRecordRepository.existsByStudentIdAndWeekId(5001L, 10L)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> visitService.create(request, servantPrincipal));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("VISIT_EXISTS", ex.getCode());
    }

    @Test
    void create_unassignedStudent_throwsForbiddenForServant() {
        VisitRequest request = new VisitRequest(5001L, 10L, VisitMethod.VISIT, 6, 7, 18, null);

        when(weekService.getWeekOrThrow(10L)).thenReturn(week);
        when(weekService.isWeekLockedForUser(week, otherServantPrincipal)).thenReturn(false);
        when(visitRecordRepository.existsByStudentIdAndWeekId(5001L, 10L)).thenReturn(false);
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L)).thenReturn(Optional.of(studentPlacement));

        AppException ex = assertThrows(AppException.class, () -> visitService.create(request, otherServantPrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void create_invalidScores_throwsBadRequest() {
        VisitRequest invalidPrayer = new VisitRequest(5001L, 10L, VisitMethod.VISIT, 8, 5, 10, null);

        when(weekService.getWeekOrThrow(10L)).thenReturn(week);
        when(weekService.isWeekLockedForUser(week, servantPrincipal)).thenReturn(false);
        when(visitRecordRepository.existsByStudentIdAndWeekId(5001L, 10L)).thenReturn(false);
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L)).thenReturn(Optional.of(studentPlacement));

        AppException ex = assertThrows(AppException.class, () -> visitService.create(invalidPrayer, servantPrincipal));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_PRAYER_SCORE", ex.getCode());
    }

    @Test
    void update_success() {
        UpdateVisitRequest request = new UpdateVisitRequest(VisitMethod.CALL, 7, 7, 20, "مكالمة");

        when(visitRecordRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(visitRecord));
        when(weekService.isWeekLockedForUser(week, servantPrincipal)).thenReturn(false);
        when(systemSettingRepository.findBySettingKey("MAX_NOTE_SCORE"))
                .thenReturn(Optional.of(new SystemSetting("MAX_NOTE_SCORE", "21")));
        when(visitRecordRepository.save(any(VisitRecord.class))).thenReturn(visitRecord);

        VisitResponse response = visitService.update(501L, request, servantPrincipal);

        assertNotNull(response);
        assertEquals("CALL", visitRecord.getMethod().name());
        assertEquals(7, visitRecord.getPrayerScore());
        assertEquals(20, visitRecord.getNoteScore());
    }

    @Test
    void update_lockedWeek_throwsForbidden() {
        UpdateVisitRequest request = new UpdateVisitRequest(VisitMethod.CALL, 7, 7, 20, "مكالمة");

        when(visitRecordRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(visitRecord));
        when(weekService.isWeekLockedForUser(week, servantPrincipal)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> visitService.update(501L, request, servantPrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("WEEK_LOCKED", ex.getCode());
    }

    @Test
    void update_adminBypassesWeekLock_success() {
        UpdateVisitRequest request = new UpdateVisitRequest(VisitMethod.CALL, 7, 7, 20, "تعديل بواسطة الأمين العام");

        when(visitRecordRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(visitRecord));
        when(weekService.isWeekLockedForUser(week, adminPrincipal)).thenReturn(false);
        when(systemSettingRepository.findBySettingKey("MAX_NOTE_SCORE"))
                .thenReturn(Optional.of(new SystemSetting("MAX_NOTE_SCORE", "21")));
        when(visitRecordRepository.save(any(VisitRecord.class))).thenReturn(visitRecord);

        VisitResponse response = visitService.update(501L, request, adminPrincipal);

        assertNotNull(response);
        verify(visitRecordRepository).save(visitRecord);
    }
}
