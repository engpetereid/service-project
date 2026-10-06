package org.serviceproject.attendance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.service.AcademicYearService;
import org.serviceproject.attendance.dto.AttendanceRecordResponse;
import org.serviceproject.attendance.dto.AttendanceSessionResponse;
import org.serviceproject.attendance.dto.CreateSessionRequest;
import org.serviceproject.attendance.dto.SessionDetailResponse;
import org.serviceproject.attendance.dto.ToggleAttendanceRequest;
import org.serviceproject.attendance.entity.ActivityType;
import org.serviceproject.attendance.entity.AttendanceRecord;
import org.serviceproject.attendance.entity.AttendanceSession;
import org.serviceproject.attendance.repository.AttendanceRecordRepository;
import org.serviceproject.attendance.repository.AttendanceSessionRepository;
import org.serviceproject.classes.entity.GradeClass;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.RoleWithScope;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.ministries.entity.Ministry;
import org.serviceproject.students.entity.StudentPlacement;
import org.serviceproject.students.entity.StudentStatus;
import org.serviceproject.students.repository.StudentPlacementRepository;
import org.serviceproject.users.entity.Gender;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.Role;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.repository.PersonRepository;
import org.serviceproject.users.repository.UserAccountRepository;
import org.serviceproject.weeks.entity.Week;
import org.serviceproject.weeks.service.WeekService;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceSessionRepository attendanceSessionRepository;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private StudentPlacementRepository studentPlacementRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private WeekService weekService;

    @Mock
    private AcademicYearService academicYearService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    private Week week;
    private AcademicYear academicYear;
    private AttendanceSession session;
    private Person student;
    private Person servant;
    private UserAccount servantAccount;
    private StudentPlacement studentPlacement;
    private UserPrincipal servantPrincipal;

    @BeforeEach
    void setUp() {
        week = new Week(LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 10));
        week.setId(10L);

        academicYear = new AcademicYear("2026/2027", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 8, 31), true);
        academicYear.setId(1L);

        session = new AttendanceSession(week, ActivityType.MASS, LocalDate.of(2026, 9, 6));
        session.setId(100L);

        student = new Person();
        student.setId(5001L);
        student.setFullName("مارك سامح");
        student.setPhone("01111111111");
        student.setGender(Gender.MALE);

        servant = new Person();
        servant.setId(3001L);
        servant.setFullName("مينا جرجس");
        servant.setPhone("01001111111");
        servant.setGender(Gender.MALE);

        servantAccount = new UserAccount();
        servantAccount.setId(201L);
        servantAccount.setPerson(servant);

        Ministry ministry = new Ministry("ابتدائي");
        ministry.setId(1000L);

        GradeClass gradeClass = new GradeClass("أولى ابتدائي", ministry);
        gradeClass.setId(2000L);

        studentPlacement = new StudentPlacement(student, academicYear, ministry, gradeClass);
        // Student is assigned to a DIFFERENT servant, or no servant
        studentPlacement.setStatus(StudentStatus.ACTIVE);

        servantPrincipal = new UserPrincipal(201L, 3001L, "01001111111", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVANT, null, null)));
    }

    @Test
    void createSession_success() {
        CreateSessionRequest request = new CreateSessionRequest(10L, ActivityType.MASS, LocalDate.of(2026, 9, 6));

        when(weekService.getWeekOrThrow(10L)).thenReturn(week);
        when(weekService.isWeekLockedForUser(week, servantPrincipal)).thenReturn(false);
        when(attendanceSessionRepository.existsByWeekIdAndActivityTypeAndSessionDate(10L, ActivityType.MASS, LocalDate.of(2026, 9, 6)))
                .thenReturn(false);

        when(attendanceSessionRepository.save(any(AttendanceSession.class))).thenAnswer(i -> {
            AttendanceSession s = i.getArgument(0);
            s.setId(101L);
            return s;
        });

        AttendanceSessionResponse res = attendanceService.createSession(request, servantPrincipal);

        assertNotNull(res);
        assertEquals(101L, res.id());
        assertEquals("MASS", res.activityType());
    }

    @Test
    void createSession_weekLocked_throwsForbidden() {
        CreateSessionRequest request = new CreateSessionRequest(10L, ActivityType.MASS, LocalDate.of(2026, 9, 6));

        when(weekService.getWeekOrThrow(10L)).thenReturn(week);
        when(weekService.isWeekLockedForUser(week, servantPrincipal)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> attendanceService.createSession(request, servantPrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("WEEK_LOCKED", ex.getCode());
    }

    @Test
    void createSession_duplicate_throwsConflict() {
        CreateSessionRequest request = new CreateSessionRequest(10L, ActivityType.MASS, LocalDate.of(2026, 9, 6));

        when(weekService.getWeekOrThrow(10L)).thenReturn(week);
        when(weekService.isWeekLockedForUser(week, servantPrincipal)).thenReturn(false);
        when(attendanceSessionRepository.existsByWeekIdAndActivityTypeAndSessionDate(10L, ActivityType.MASS, LocalDate.of(2026, 9, 6)))
                .thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> attendanceService.createSession(request, servantPrincipal));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("SESSION_EXISTS", ex.getCode());
    }

    @Test
    void createSession_notCurrentWeek_nonAdmin_throwsBadRequest() {
        CreateSessionRequest request = new CreateSessionRequest(10L, ActivityType.MASS, LocalDate.of(2026, 9, 6));

        Week otherWeek = new Week(LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 17));
        otherWeek.setId(11L);

        when(weekService.getWeekOrThrow(10L)).thenReturn(week);
        when(weekService.getCurrentWeekEntity()).thenReturn(otherWeek);

        AppException ex = assertThrows(AppException.class, () -> attendanceService.createSession(request, servantPrincipal));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("CURRENT_WEEK_ONLY", ex.getCode());
    }

    @Test
    void toggleAttendance_scopeException_servantCanRecordForAnyActiveStudent() {
        // Explicit architectural requirement: Any servant can record attendance for any active student
        ToggleAttendanceRequest request = new ToggleAttendanceRequest(100L, 5001L, true);

        when(attendanceSessionRepository.findByIdWithWeek(100L)).thenReturn(Optional.of(session));
        when(weekService.isWeekLockedForUser(week, servantPrincipal)).thenReturn(false);
        when(personRepository.findByIdAndDeletedAtIsNull(5001L)).thenReturn(Optional.of(student));
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L)).thenReturn(Optional.of(studentPlacement));
        when(userAccountRepository.findById(201L)).thenReturn(Optional.of(servantAccount));
        when(attendanceRecordRepository.findBySessionIdAndStudentId(100L, 5001L)).thenReturn(Optional.empty());

        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(i -> {
            AttendanceRecord ar = i.getArgument(0);
            ar.setId(901L);
            return ar;
        });

        AttendanceRecordResponse response = attendanceService.toggleAttendance(request, servantPrincipal);

        assertNotNull(response);
        assertTrue(response.present());
        assertEquals(5001L, response.studentId());
        assertEquals("مارك سامح", response.studentName());
        verify(attendanceRecordRepository).save(any(AttendanceRecord.class));
    }

    @Test
    void toggleAttendance_weekLocked_throwsForbidden() {
        ToggleAttendanceRequest request = new ToggleAttendanceRequest(100L, 5001L, true);

        when(attendanceSessionRepository.findByIdWithWeek(100L)).thenReturn(Optional.of(session));
        when(weekService.isWeekLockedForUser(week, servantPrincipal)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> attendanceService.toggleAttendance(request, servantPrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("WEEK_LOCKED", ex.getCode());
    }

    @Test
    void toggleAttendance_studentInactive_throwsBadRequest() {
        ToggleAttendanceRequest request = new ToggleAttendanceRequest(100L, 5001L, true);
        studentPlacement.setStatus(StudentStatus.GRADUATED);

        when(attendanceSessionRepository.findByIdWithWeek(100L)).thenReturn(Optional.of(session));
        when(weekService.isWeekLockedForUser(week, servantPrincipal)).thenReturn(false);
        when(personRepository.findByIdAndDeletedAtIsNull(5001L)).thenReturn(Optional.of(student));
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L)).thenReturn(Optional.of(studentPlacement));

        AppException ex = assertThrows(AppException.class, () -> attendanceService.toggleAttendance(request, servantPrincipal));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("STUDENT_NOT_ACTIVE", ex.getCode());
    }

    @Test
    void getSessionDetails_success() {
        when(attendanceSessionRepository.findByIdWithWeek(100L)).thenReturn(Optional.of(session));

        AttendanceRecord record = new AttendanceRecord(session, student, true, servantAccount);
        record.setId(901L);
        when(attendanceRecordRepository.findAllBySessionId(100L)).thenReturn(List.of(record));

        SessionDetailResponse res = attendanceService.getSessionDetails(100L);

        assertNotNull(res);
        assertEquals(100L, res.session().id());
        assertEquals(1, res.records().size());
        assertTrue(res.records().get(0).present());
    }
}
