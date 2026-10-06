package org.serviceproject.statistics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.service.AcademicYearService;
import org.serviceproject.attendance.entity.ActivityType;
import org.serviceproject.attendance.entity.AttendanceRecord;
import org.serviceproject.attendance.entity.AttendanceSession;
import org.serviceproject.attendance.repository.AttendanceRecordRepository;
import org.serviceproject.classes.entity.GradeClass;
import org.serviceproject.classes.repository.GradeClassRepository;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.RoleWithScope;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.ministries.entity.Ministry;
import org.serviceproject.ministries.repository.MinistryRepository;
import org.serviceproject.staff.entity.StaffPlacement;
import org.serviceproject.staff.repository.StaffPlacementRepository;
import org.serviceproject.confession.entity.ConfessionRecord;
import org.serviceproject.confession.repository.ConfessionRecordRepository;
import org.serviceproject.statistics.dto.AbsenceAlertResponse;
import org.serviceproject.statistics.dto.ClassStatisticsResponse;
import org.serviceproject.statistics.dto.DashboardStatisticsResponse;
import org.serviceproject.statistics.dto.MinistryStatisticsResponse;
import org.serviceproject.statistics.dto.ServantPerformanceResponse;
import org.serviceproject.statistics.dto.ServantStatisticsResponse;
import org.serviceproject.statistics.dto.StudentStatisticsResponse;
import org.serviceproject.statistics.dto.WeeklyTrendDataPoint;
import org.serviceproject.students.entity.StudentPlacement;
import org.serviceproject.students.entity.StudentStatus;
import org.serviceproject.students.repository.StudentPlacementRepository;
import org.serviceproject.users.entity.Gender;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.Role;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.repository.PersonRepository;
import org.serviceproject.users.repository.UserAccountRepository;
import org.serviceproject.selffollowup.repository.ServantWeeklyFollowUpRepository;
import org.serviceproject.visits.entity.VisitMethod;
import org.serviceproject.visits.entity.VisitRecord;
import org.serviceproject.visits.repository.VisitRecordRepository;
import org.serviceproject.weeks.entity.Week;
import org.serviceproject.weeks.repository.WeekRepository;
import org.serviceproject.weeks.service.WeekService;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private VisitRecordRepository visitRecordRepository;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private ConfessionRecordRepository confessionRecordRepository;

    @Mock
    private StudentPlacementRepository studentPlacementRepository;

    @Mock
    private StaffPlacementRepository staffPlacementRepository;

    @Mock
    private MinistryRepository ministryRepository;

    @Mock
    private GradeClassRepository gradeClassRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private WeekRepository weekRepository;

    @Mock
    private WeekService weekService;

    @Mock
    private AcademicYearService academicYearService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private ServantWeeklyFollowUpRepository servantWeeklyFollowUpRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    private AcademicYear academicYear;
    private Week week10;
    private Week week9;
    private Ministry ministry;
    private GradeClass gradeClass;
    private Person student1;
    private Person student2;
    private Person servant1;
    private UserAccount servantAccount;
    private StudentPlacement placement1;
    private StudentPlacement placement2;

    private UserPrincipal adminPrincipal;
    private UserPrincipal ministrySecretaryPrincipal;
    private UserPrincipal classSecretaryPrincipal;
    private UserPrincipal servantPrincipal;
    private UserPrincipal otherServantPrincipal;

    @BeforeEach
    void setUp() {
        academicYear = new AcademicYear("2026/2027", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 8, 31), true);
        academicYear.setId(1L);

        week10 = new Week(LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 17));
        week10.setId(10L);

        week9 = new Week(LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 10));
        week9.setId(9L);

        ministry = new Ministry("ابتدائي");
        ministry.setId(100L);

        gradeClass = new GradeClass("أولى ابتدائي", ministry);
        gradeClass.setId(1000L);

        student1 = new Person();
        student1.setId(5001L);
        student1.setFullName("مارك ماجد");
        student1.setPhone("01111111111");
        student1.setGender(Gender.MALE);

        student2 = new Person();
        student2.setId(5002L);
        student2.setFullName("توماس مينا");
        student2.setPhone("01111111112");
        student2.setGender(Gender.MALE);

        servant1 = new Person();
        servant1.setId(3001L);
        servant1.setFullName("مينا جرجس");
        servant1.setPhone("01001111111");
        servant1.setGender(Gender.MALE);

        servantAccount = new UserAccount();
        servantAccount.setId(201L);
        servantAccount.setPerson(servant1);

        placement1 = new StudentPlacement(student1, academicYear, ministry, gradeClass);
        placement1.assignServant(servant1);

        placement2 = new StudentPlacement(student2, academicYear, ministry, gradeClass);
        placement2.assignServant(servant1);

        adminPrincipal = new UserPrincipal(101L, 999L, "01000000000", "pass", true, 0,
                Set.of(new RoleWithScope(Role.GENERAL_ADMIN, null, null)));

        ministrySecretaryPrincipal = new UserPrincipal(102L, 998L, "01000000001", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVICE_SECRETARY, 100L, null)));

        classSecretaryPrincipal = new UserPrincipal(103L, 997L, "01000000002", "pass", true, 0,
                Set.of(new RoleWithScope(Role.CLASS_SECRETARY, null, 1000L)));

        servantPrincipal = new UserPrincipal(201L, 3001L, "01001111111", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVANT, null, null)));

        otherServantPrincipal = new UserPrincipal(301L, 3002L, "01002222222", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVANT, null, null)));
    }

    @Test
    void getDashboard_zeroStudents_returnsZeroPercentagesWithoutDivisionByZero() {
        when(weekService.getCurrentWeekEntity()).thenReturn(week10);
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findAllByAcademicYearIdAndStatus(1L, StudentStatus.ACTIVE))
                .thenReturn(Collections.emptyList());
        when(visitRecordRepository.findAllByWeekId(10L)).thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.findAllPresentByWeekIdAndActivityType(10L, ActivityType.MASS))
                .thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.findAllPresentByWeekIdAndActivityType(10L, ActivityType.MEETING))
                .thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.findAllPresentByWeekIdAndActivityType(10L, ActivityType.TASBEHA))
                .thenReturn(Collections.emptyList());

        DashboardStatisticsResponse response = statisticsService.getDashboard(null, adminPrincipal);

        assertNotNull(response);
        assertEquals(10L, response.weekId());
        assertEquals(0, response.totalStudents());
        assertEquals(0, response.visitedStudents());
        assertEquals(0.0, response.visitPercentage());
        assertEquals(0, response.massAttendanceCount());
        assertEquals(0.0, response.massAttendancePercentage());
        assertEquals(0, response.meetingAttendanceCount());
        assertEquals(0.0, response.meetingAttendancePercentage());
        assertEquals(0, response.tasbehaAttendanceCount());
        assertEquals(0.0, response.tasbehaAttendancePercentage());
        assertEquals(0.0, response.overallFollowupIndex());
        assertEquals(0, response.totalAbsenceAlerts());
    }

    @Test
    void getDashboard_withStudentsAndActivities_calculatesWeightedIndexCorrectly() {
        when(weekService.getWeekOrThrow(10L)).thenReturn(week10);
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findAllByAcademicYearIdAndStatus(1L, StudentStatus.ACTIVE))
                .thenReturn(List.of(placement1, placement2));

        // 1 visit for student1 -> 50.0%
        VisitRecord vr1 = new VisitRecord(student1, week10, academicYear, ministry, gradeClass, servant1,
                VisitMethod.VISIT, 5, 5, 10, null, servantAccount);
        when(visitRecordRepository.findAllByWeekId(10L)).thenReturn(List.of(vr1));

        // 2 mass attendances -> 100.0%
        AttendanceSession massSession = new AttendanceSession(week10, ActivityType.MASS, week10.getStartDate());
        AttendanceRecord arMass1 = new AttendanceRecord(massSession, student1, true, servantAccount);
        AttendanceRecord arMass2 = new AttendanceRecord(massSession, student2, true, servantAccount);
        when(attendanceRecordRepository.findAllPresentByWeekIdAndActivityType(10L, ActivityType.MASS))
                .thenReturn(List.of(arMass1, arMass2));

        // 1 meeting attendance -> 50.0%
        AttendanceSession meetingSession = new AttendanceSession(week10, ActivityType.MEETING, week10.getStartDate());
        AttendanceRecord arMeet1 = new AttendanceRecord(meetingSession, student1, true, servantAccount);
        when(attendanceRecordRepository.findAllPresentByWeekIdAndActivityType(10L, ActivityType.MEETING))
                .thenReturn(List.of(arMeet1));

        // 0 tasbeha attendance -> 0.0%
        when(attendanceRecordRepository.findAllPresentByWeekIdAndActivityType(10L, ActivityType.TASBEHA))
                .thenReturn(Collections.emptyList());

        // For absence alerts count check
        when(weekRepository.findAllByDeletedAtIsNullOrderByStartDateDesc())
                .thenReturn(List.of(week10, week9));

        DashboardStatisticsResponse response = statisticsService.getDashboard(10L, adminPrincipal);

        assertNotNull(response);
        assertEquals(2, response.totalStudents());
        assertEquals(1, response.visitedStudents());
        assertEquals(50.0, response.visitPercentage());
        assertEquals(2, response.massAttendanceCount());
        assertEquals(100.0, response.massAttendancePercentage());
        assertEquals(1, response.meetingAttendanceCount());
        assertEquals(50.0, response.meetingAttendancePercentage());
        assertEquals(0, response.tasbehaAttendanceCount());
        assertEquals(0.0, response.tasbehaAttendancePercentage());

        // Expected index = (0.40 * 50) + (0.25 * 100) + (0.25 * 50) + (0.10 * 0)
        // = 20.0 + 25.0 + 12.5 + 0.0 = 57.5
        assertEquals(57.5, response.overallFollowupIndex());
    }

    @Test
    void getMinistryStatistics_validScope_returnsSummaryAndClasses() {
        when(ministryRepository.findById(100L)).thenReturn(Optional.of(ministry));
        when(weekService.getCurrentWeekEntity()).thenReturn(week10);
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);

        when(studentPlacementRepository.findAllByAcademicYearIdAndMinistryIdAndStatus(1L, 100L, StudentStatus.ACTIVE))
                .thenReturn(List.of(placement1, placement2));

        when(gradeClassRepository.findAllByMinistryIdAndActiveTrueOrderBySortOrderAsc(100L))
                .thenReturn(List.of(gradeClass));

        when(weekRepository.findAllByDeletedAtIsNullOrderByStartDateDesc())
                .thenReturn(List.of(week10, week9));

        when(visitRecordRepository.findAllByWeekId(10L)).thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.findAllPresentByWeekIdAndActivityType(10L, ActivityType.MASS))
                .thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.findAllPresentByWeekIdAndActivityType(10L, ActivityType.MEETING))
                .thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.findAllPresentByWeekIdAndActivityType(10L, ActivityType.TASBEHA))
                .thenReturn(Collections.emptyList());

        MinistryStatisticsResponse response = statisticsService.getMinistryStatistics(100L, null, ministrySecretaryPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.ministryId());
        assertEquals("ابتدائي", response.ministryName());
        assertEquals(1, response.classesStats().size());
        assertEquals("أولى ابتدائي", response.classesStats().get(0).className());
    }

    @Test
    void getMinistryStatistics_outsideScope_throwsForbidden() {
        AppException ex = assertThrows(AppException.class, () ->
                statisticsService.getMinistryStatistics(999L, null, ministrySecretaryPrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void getClassStatistics_validScope_returnsServantSummaries() {
        when(gradeClassRepository.findByIdWithMinistry(1000L)).thenReturn(Optional.of(gradeClass));
        when(weekService.getCurrentWeekEntity()).thenReturn(week10);
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);

        when(studentPlacementRepository.findAllByAcademicYearIdAndClassIdAndStatus(1L, 1000L, StudentStatus.ACTIVE))
                .thenReturn(List.of(placement1, placement2));

        StaffPlacement staffPlacement = new StaffPlacement(servant1, academicYear, ministry, gradeClass);
        when(staffPlacementRepository.findAllActiveByAcademicYearIdAndClassId(1L, 1000L))
                .thenReturn(List.of(staffPlacement));

        VisitRecord vr = new VisitRecord(student1, week10, academicYear, ministry, gradeClass, servant1,
                VisitMethod.VISIT, 6, 6, 12, null, servantAccount);
        when(visitRecordRepository.findAllByWeekIdAndServantSnapId(10L, 3001L)).thenReturn(List.of(vr));

        when(weekRepository.findAllByDeletedAtIsNullOrderByStartDateDesc())
                .thenReturn(List.of(week10, week9));

        when(visitRecordRepository.findAllByWeekId(10L)).thenReturn(List.of(vr));
        when(attendanceRecordRepository.findAllPresentByWeekIdAndActivityType(10L, ActivityType.MASS))
                .thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.findAllPresentByWeekIdAndActivityType(10L, ActivityType.MEETING))
                .thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.findAllPresentByWeekIdAndActivityType(10L, ActivityType.TASBEHA))
                .thenReturn(Collections.emptyList());
        when(userAccountRepository.findAllByPersonIdIn(any()))
                .thenReturn(List.of(servantAccount));
        when(servantWeeklyFollowUpRepository.findAllByWeekIdAndUserIdIn(eq(10L), any()))
                .thenReturn(Collections.emptyList());

        ClassStatisticsResponse response = statisticsService.getClassStatistics(1000L, null, classSecretaryPrincipal);

        assertNotNull(response);
        assertEquals(1000L, response.classId());
        assertEquals("أولى ابتدائي", response.className());
        assertEquals(1, response.servantsStats().size());
        assertEquals(3001L, response.servantsStats().get(0).servantId());
        assertEquals(2, response.servantsStats().get(0).assignedStudentsCount());
        assertEquals(1, response.servantsStats().get(0).visitedCount());
        assertEquals(50.0, response.servantsStats().get(0).visitPercentage());
    }

    @Test
    void getClassStatistics_outsideScope_throwsForbidden() {
        GradeClass otherClass = new GradeClass("ثانية ابتدائي", ministry);
        otherClass.setId(2000L);

        when(gradeClassRepository.findByIdWithMinistry(2000L)).thenReturn(Optional.of(otherClass));

        AppException ex = assertThrows(AppException.class, () ->
                statisticsService.getClassStatistics(2000L, null, classSecretaryPrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void getServantStatistics_success_calculatesAverages() {
        when(personRepository.findByIdAndDeletedAtIsNull(3001L)).thenReturn(Optional.of(servant1));
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(weekService.getCurrentWeekEntity()).thenReturn(week10);

        when(studentPlacementRepository.findAllByAcademicYearIdAndServantIdAndStatus(1L, 3001L, StudentStatus.ACTIVE))
                .thenReturn(List.of(placement1, placement2));

        VisitRecord vr1 = new VisitRecord(student1, week10, academicYear, ministry, gradeClass, servant1,
                VisitMethod.VISIT, 5, 6, 10, null, servantAccount);
        VisitRecord vr2 = new VisitRecord(student2, week10, academicYear, ministry, gradeClass, servant1,
                VisitMethod.CALL, 7, 8, 14, null, servantAccount);

        when(visitRecordRepository.findAllByWeekIdAndServantSnapId(10L, 3001L)).thenReturn(List.of(vr1, vr2));
        when(userAccountRepository.findByPersonId(3001L)).thenReturn(Optional.of(servantAccount));
        when(servantWeeklyFollowUpRepository.findByUserIdAndWeekId(201L, 10L)).thenReturn(Optional.empty());
        when(servantWeeklyFollowUpRepository.findAllByUserIdAndAcademicYearIdOrderByWeekStartDateDesc(201L, 1L))
                .thenReturn(Collections.emptyList());
        when(weekRepository.findAllByDeletedAtIsNullOrderByStartDateDesc()).thenReturn(List.of(week10, week9));

        ServantStatisticsResponse response = statisticsService.getServantStatistics(3001L, null, servantPrincipal);

        assertNotNull(response);
        assertEquals(3001L, response.servantId());
        assertEquals("مينا جرجس", response.servantName());
        assertEquals(2, response.assignedStudentsCount());
        assertEquals(2, response.visitedCount());
        assertEquals(100.0, response.visitPercentage());
        assertEquals(6.0, response.averagePrayerScore());
        assertEquals(7.0, response.averageReadingScore());
        assertEquals(12.0, response.averageNoteScore());
    }

    @Test
    void getServantStatistics_outsideScope_throwsForbidden() {
        when(personRepository.findByIdAndDeletedAtIsNull(3001L)).thenReturn(Optional.of(servant1));
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);

        StaffPlacement sp = new StaffPlacement(servant1, academicYear, ministry, gradeClass);
        when(staffPlacementRepository.findByPersonIdAndAcademicYearId(3001L, 1L)).thenReturn(Optional.of(sp));

        AppException ex = assertThrows(AppException.class, () ->
                statisticsService.getServantStatistics(3001L, null, otherServantPrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void getAbsenceAlerts_detectsStudentsMissingVisitsAndMeetings() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findAllByAcademicYearIdAndStatus(1L, StudentStatus.ACTIVE))
                .thenReturn(List.of(placement1, placement2));

        when(weekRepository.findAllByDeletedAtIsNullOrderByStartDateDesc())
                .thenReturn(List.of(week10, week9));

        List<Long> weekIds = List.of(10L, 9L);

        // Student1 has NO visits and NO meetings in weekIds -> absent
        when(visitRecordRepository.findAllByStudentIdAndWeekIds(5001L, weekIds)).thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.findAllPresentByStudentIdAndWeekIds(5001L, weekIds)).thenReturn(Collections.emptyList());

        // Student2 had a visit in week 10 -> present
        VisitRecord vrStudent2 = new VisitRecord(student2, week10, academicYear, ministry, gradeClass, servant1,
                VisitMethod.VISIT, 6, 6, 12, null, servantAccount);
        when(visitRecordRepository.findAllByStudentIdAndWeekIds(5002L, weekIds)).thenReturn(List.of(vrStudent2));
        when(attendanceRecordRepository.findAllPresentByStudentIdAndWeekIds(5002L, weekIds)).thenReturn(Collections.emptyList());

        List<AbsenceAlertResponse> alerts = statisticsService.getAbsenceAlerts(adminPrincipal, 2);

        assertNotNull(alerts);
        assertEquals(1, alerts.size());
        assertEquals(5001L, alerts.get(0).studentId());
        assertEquals("مارك ماجد", alerts.get(0).studentName());
        assertEquals(2, alerts.get(0).consecutiveWeeksAbsent());
    }

    @Test
    void getAbsenceAlerts_notEnoughWeeks_returnsEmptyList() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findAllByAcademicYearIdAndStatus(1L, StudentStatus.ACTIVE))
                .thenReturn(List.of(placement1));

        // Only 1 past week available, but threshold is 2
        when(weekRepository.findAllByDeletedAtIsNullOrderByStartDateDesc())
                .thenReturn(List.of(week10));

        List<AbsenceAlertResponse> alerts = statisticsService.getAbsenceAlerts(adminPrincipal, 2);

        assertNotNull(alerts);
        assertTrue(alerts.isEmpty());
    }

    @Test
    void getTrends_success_calculatesWeeklyTrendsChronologically() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findAllByAcademicYearIdAndStatus(1L, StudentStatus.ACTIVE))
                .thenReturn(List.of(placement1));

        // Returns week10 and week9 in descending order
        when(weekRepository.findAllByDeletedAtIsNullOrderByStartDateDesc())
                .thenReturn(List.of(week10, week9));

        when(visitRecordRepository.findAllByWeekId(anyLong())).thenReturn(Collections.emptyList());
        when(attendanceRecordRepository.findAllPresentByWeekIdAndActivityType(anyLong(), any()))
                .thenReturn(Collections.emptyList());

        List<WeeklyTrendDataPoint> trends = statisticsService.getTrends(4, adminPrincipal);

        assertNotNull(trends);
        assertEquals(2, trends.size());
        // Chronological order: week9 first, then week10
        assertEquals(9L, trends.get(0).weekId());
        assertEquals(10L, trends.get(1).weekId());
    }

    @Test
    void getStudentStatistics_validScope_calculatesAveragesAndAttendance() {
        when(personRepository.findByIdAndDeletedAtIsNull(5001L)).thenReturn(Optional.of(student1));
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L))
                .thenReturn(Optional.of(placement1));

        when(weekRepository.findAllByDeletedAtIsNullOrderByStartDateDesc())
                .thenReturn(List.of(week10, week9));

        VisitRecord vr1 = new VisitRecord(student1, week10, academicYear, ministry, gradeClass, servant1,
                VisitMethod.VISIT, 6, 7, 14, "متابع جيد", servantAccount);
        when(visitRecordRepository.findAllByStudentIdOrderByWeekDesc(5001L))
                .thenReturn(List.of(vr1));

        AttendanceSession massSession = new AttendanceSession(week10, ActivityType.MASS, week10.getStartDate());
        AttendanceRecord arMass = new AttendanceRecord(massSession, student1, true, servantAccount);
        when(attendanceRecordRepository.findAllPresentByStudentId(5001L))
                .thenReturn(List.of(arMass));

        ConfessionRecord cr = new ConfessionRecord(student1, academicYear, LocalDate.of(2026, 9, 15), "أبونا أنطونيوس", "اعتراف دوري", servantAccount);
        when(confessionRecordRepository.findAllByStudentIdAndAcademicYearId(5001L, 1L))
                .thenReturn(List.of(cr));

        StudentStatisticsResponse response = statisticsService.getStudentStatistics(5001L, adminPrincipal);

        assertNotNull(response);
        assertEquals(5001L, response.studentId());
        assertEquals("مارك ماجد", response.studentName());
        assertEquals(2, response.totalWeeksCount());
        assertEquals(1, response.visitedWeeksCount());
        assertEquals(50.0, response.visitPercentage());
        assertEquals(1, response.massAttendanceCount());
        assertEquals(50.0, response.massAttendancePercentage());
        assertEquals(0, response.meetingAttendanceCount());
        assertEquals(0.0, response.meetingAttendancePercentage());
        assertEquals(6.0, response.averagePrayerScore());
        assertEquals(7.0, response.averageReadingScore());
        assertEquals(14.0, response.averageNoteScore());
        assertEquals(1, response.totalConfessionsCount());
        assertEquals(LocalDate.of(2026, 9, 15), response.lastConfessionDate());
        assertEquals(1, response.recentVisits().size());
        assertEquals("متابع جيد", response.recentVisits().get(0).notes());
    }

    @Test
    void getStudentStatistics_outsideScope_throwsForbidden() {
        when(personRepository.findByIdAndDeletedAtIsNull(5001L)).thenReturn(Optional.of(student1));
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findByPersonIdAndAcademicYearId(5001L, 1L))
                .thenReturn(Optional.of(placement1));

        // otherServantPrincipal is assigned to neither placement1 nor its class
        AppException ex = assertThrows(AppException.class, () ->
                statisticsService.getStudentStatistics(5001L, otherServantPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void getServantsPerformance_success() {
        when(weekService.getCurrentWeekEntity()).thenReturn(week10);
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);

        StaffPlacement staffPlacement = new StaffPlacement(servant1, academicYear, ministry, gradeClass);
        when(staffPlacementRepository.findAllActiveByAcademicYearId(1L))
                .thenReturn(List.of(staffPlacement));
        when(userAccountRepository.findAllByPersonIdIn(List.of(3001L)))
                .thenReturn(List.of(servantAccount));
        when(servantWeeklyFollowUpRepository.findAllByWeekIdAndUserIdIn(eq(10L), any()))
                .thenReturn(Collections.emptyList());
        when(studentPlacementRepository.findAllByAcademicYearIdAndStatus(1L, StudentStatus.ACTIVE))
                .thenReturn(List.of(placement1, placement2));
        when(visitRecordRepository.findAllByWeekId(10L))
                .thenReturn(Collections.emptyList());

        ServantPerformanceResponse response = statisticsService.getServantsPerformance(null, null, null, adminPrincipal);

        assertNotNull(response);
        assertEquals(1, response.totalServants());
        assertEquals(0, response.recordedFollowUpCount());
        assertEquals(1, response.servants().size());
        assertEquals(3001L, response.servants().get(0).servantId());
    }
}
