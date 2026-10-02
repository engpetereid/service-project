package org.serviceproject.reports.service;

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
import org.serviceproject.attendance.repository.AttendanceSessionRepository;
import org.serviceproject.classes.entity.GradeClass;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.RoleWithScope;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.confession.entity.ConfessionRecord;
import org.serviceproject.confession.repository.ConfessionRecordRepository;
import org.serviceproject.ministries.entity.Ministry;
import org.serviceproject.reports.dto.AttendanceReportFilter;
import org.serviceproject.reports.dto.ConfessionReportFilter;
import org.serviceproject.reports.dto.StudentReportFilter;
import org.serviceproject.reports.dto.VisitReportFilter;
import org.serviceproject.students.entity.StudentPlacement;
import org.serviceproject.students.entity.StudentStatus;
import org.serviceproject.students.repository.StudentPlacementRepository;
import org.serviceproject.users.entity.Gender;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.Role;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.visits.entity.VisitMethod;
import org.serviceproject.visits.entity.VisitRecord;
import org.serviceproject.visits.repository.VisitRecordRepository;
import org.serviceproject.weeks.entity.Week;
import org.serviceproject.weeks.service.WeekService;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private StudentPlacementRepository studentPlacementRepository;

    @Mock
    private VisitRecordRepository visitRecordRepository;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private AttendanceSessionRepository attendanceSessionRepository;

    @Mock
    private ConfessionRecordRepository confessionRecordRepository;

    @Mock
    private AcademicYearService academicYearService;

    @Mock
    private WeekService weekService;

    @InjectMocks
    private ReportService reportService;

    private AcademicYear academicYear;
    private Week week;
    private Ministry ministry1;
    private Ministry ministry2;
    private GradeClass gradeClass1;
    private GradeClass gradeClass2;
    private Person student1;
    private Person student2;
    private Person servant1;
    private Person servant2;
    private UserAccount recorderAccount;
    private StudentPlacement placement1;
    private StudentPlacement placement2;

    private UserPrincipal adminPrincipal;
    private UserPrincipal serviceSecretaryPrincipal;
    private UserPrincipal servantPrincipal;

    @BeforeEach
    void setUp() {
        academicYear = new AcademicYear("2026/2027", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 8, 31), true);
        academicYear.setId(1L);

        week = new Week(LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 10));
        week.setId(10L);

        ministry1 = new Ministry("ابتدائي");
        ministry1.setId(100L);

        ministry2 = new Ministry("إعدادي");
        ministry2.setId(200L);

        gradeClass1 = new GradeClass("أولى ابتدائي", ministry1);
        gradeClass1.setId(1000L);

        gradeClass2 = new GradeClass("أولى إعدادي", ministry2);
        gradeClass2.setId(2000L);

        servant1 = new Person();
        servant1.setId(3001L);
        servant1.setFullName("مينا جرجس");
        servant1.setPhone("01001111111");
        servant1.setGender(Gender.MALE);

        servant2 = new Person();
        servant2.setId(3002L);
        servant2.setFullName("بيتر ماجد");
        servant2.setPhone("01002222222");
        servant2.setGender(Gender.MALE);

        recorderAccount = new UserAccount();
        recorderAccount.setId(201L);
        recorderAccount.setPerson(servant1);

        student1 = new Person();
        student1.setId(5001L);
        student1.setFullName("مارك سامح");
        student1.setPhone("01111111111");
        student1.setGender(Gender.MALE);
        student1.setDateOfBirth(LocalDate.of(2015, 5, 20));
        student1.setAddress("شبرا");
        student1.setConfessionFather("أبونا أنطونيوس");

        student2 = new Person();
        student2.setId(5002L);
        student2.setFullName("مارينا يوسف");
        student2.setPhone("01122222222");
        student2.setGender(Gender.FEMALE);

        placement1 = new StudentPlacement(student1, academicYear, ministry1, gradeClass1);
        placement1.assignServant(servant1);
        placement1.setGuardianPhone("01233333333");
        placement1.setTalents("رسم وموسيقى");

        placement2 = new StudentPlacement(student2, academicYear, ministry2, gradeClass2);
        placement2.assignServant(servant2);

        adminPrincipal = new UserPrincipal(101L, 999L, "01000000000", "pass", true, 0,
                Set.of(new RoleWithScope(Role.GENERAL_ADMIN, null, null)));

        serviceSecretaryPrincipal = new UserPrincipal(102L, 998L, "01000000001", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVICE_SECRETARY, 100L, null)));

        servantPrincipal = new UserPrincipal(201L, 3001L, "01001111111", "pass", true, 0,
                Set.of(new RoleWithScope(Role.SERVANT, null, null)));
    }

    @Test
    void exportStudentsCsv_admin_returnsAllStudents() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findAllByAcademicYearId(1L)).thenReturn(List.of(placement1, placement2));

        byte[] csvBytes = reportService.exportStudentsCsv(null, adminPrincipal);

        assertNotNull(csvBytes);
        String csv = new String(csvBytes, StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("\uFEFF")); // UTF-8 BOM
        assertTrue(csv.contains("كود المخدوم,الاسم الكامل"));
        assertTrue(csv.contains("مارك سامح"));
        assertTrue(csv.contains("مارينا يوسف"));
        assertTrue(csv.contains("ابتدائي"));
        assertTrue(csv.contains("إعدادي"));
    }

    @Test
    void exportStudentsCsv_serviceSecretary_scopeEnforced() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);
        when(studentPlacementRepository.findAllByAcademicYearId(1L)).thenReturn(List.of(placement1, placement2));

        byte[] csvBytes = reportService.exportStudentsCsv(null, serviceSecretaryPrincipal);

        assertNotNull(csvBytes);
        String csv = new String(csvBytes, StandardCharsets.UTF_8);

        assertTrue(csv.contains("مارك سامح"));
        assertFalse(csv.contains("مارينا يوسف")); // In ministry2 (200), outside secretary scope
    }

    @Test
    void exportStudentsCsv_outsideScope_throwsForbidden() {
        StudentReportFilter filter = new StudentReportFilter(null, 200L, null, null, null);

        AppException ex = assertThrows(AppException.class, () ->
                reportService.exportStudentsCsv(filter, serviceSecretaryPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void exportVisitsCsv_servant_scopeEnforced() {
        when(weekService.getCurrentWeekEntity()).thenReturn(week);

        VisitRecord vr1 = new VisitRecord(student1, week, academicYear, ministry1, gradeClass1, servant1,
                VisitMethod.VISIT, 6, 7, 20, "زيارة", recorderAccount);

        VisitRecord vr2 = new VisitRecord(student2, week, academicYear, ministry2, gradeClass2, servant2,
                VisitMethod.CALL, 5, 5, 15, "مكالمة", recorderAccount);

        when(visitRecordRepository.findAllByWeekId(10L)).thenReturn(List.of(vr1, vr2));

        byte[] csvBytes = reportService.exportVisitsCsv(null, servantPrincipal);

        assertNotNull(csvBytes);
        String csv = new String(csvBytes, StandardCharsets.UTF_8);

        assertTrue(csv.contains("مارك سامح"));
        assertTrue(csv.contains("زيارة"));
        assertFalse(csv.contains("مارينا يوسف")); // Servant 2's visit
    }

    @Test
    void exportVisitsCsv_outsideScope_throwsForbidden() {
        VisitReportFilter filter = new VisitReportFilter(null, 10L, null, null, 3002L);

        AppException ex = assertThrows(AppException.class, () ->
                reportService.exportVisitsCsv(filter, servantPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void exportAttendanceCsv_success() {
        when(weekService.getCurrentWeekEntity()).thenReturn(week);
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);

        AttendanceSession session = new AttendanceSession(week, ActivityType.MASS, week.getStartDate());
        session.setId(501L);

        AttendanceRecord record = new AttendanceRecord(session, student1, true, recorderAccount);

        when(attendanceSessionRepository.findAllByWeekId(10L)).thenReturn(List.of(session));
        when(studentPlacementRepository.findAllByAcademicYearIdAndStatus(1L, StudentStatus.ACTIVE))
                .thenReturn(List.of(placement1));
        when(attendanceRecordRepository.findAllBySessionId(501L)).thenReturn(List.of(record));

        byte[] csvBytes = reportService.exportAttendanceCsv(null, adminPrincipal);

        assertNotNull(csvBytes);
        String csv = new String(csvBytes, StandardCharsets.UTF_8);

        assertTrue(csv.contains("قداس"));
        assertTrue(csv.contains("مارك سامح"));
        assertTrue(csv.contains("حاضر"));
    }

    @Test
    void exportConfessionsCsv_success() {
        when(academicYearService.getCurrentEntity()).thenReturn(academicYear);

        ConfessionRecord cr = new ConfessionRecord(student1, academicYear, LocalDate.of(2026, 9, 8),
                "أبونا أنطونيوس", "اعتراف دوري", recorderAccount);
        cr.setId(801L);

        when(confessionRecordRepository.findAllByAcademicYearId(1L)).thenReturn(List.of(cr));
        when(studentPlacementRepository.findAllByAcademicYearIdAndStatus(1L, StudentStatus.ACTIVE))
                .thenReturn(List.of(placement1));

        byte[] csvBytes = reportService.exportConfessionsCsv(null, adminPrincipal);

        assertNotNull(csvBytes);
        String csv = new String(csvBytes, StandardCharsets.UTF_8);

        assertTrue(csv.contains("كود الاعتراف,العام الدراسي"));
        assertTrue(csv.contains("مارك سامح"));
        assertTrue(csv.contains("أبونا أنطونيوس"));
        assertTrue(csv.contains("اعتراف دوري"));
    }
}
