package org.serviceproject.statistics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.service.AcademicYearService;
import org.serviceproject.attendance.entity.ActivityType;
import org.serviceproject.attendance.entity.AttendanceRecord;
import org.serviceproject.attendance.repository.AttendanceRecordRepository;
import org.serviceproject.classes.entity.GradeClass;
import org.serviceproject.classes.repository.GradeClassRepository;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.common.util.DateUtil;
import org.serviceproject.ministries.entity.Ministry;
import org.serviceproject.ministries.repository.MinistryRepository;
import org.serviceproject.staff.entity.StaffPlacement;
import org.serviceproject.staff.repository.StaffPlacementRepository;
import org.serviceproject.confession.entity.ConfessionRecord;
import org.serviceproject.confession.repository.ConfessionRecordRepository;
import org.serviceproject.statistics.dto.AbsenceAlertResponse;
import org.serviceproject.statistics.dto.AdminSetupResponse;
import org.serviceproject.statistics.dto.ClassStatisticsResponse;
import org.serviceproject.statistics.dto.ClassStatisticsSummary;
import org.serviceproject.statistics.dto.DashboardStatisticsResponse;
import org.serviceproject.statistics.dto.MinistryStatisticsResponse;
import org.serviceproject.statistics.dto.ServantStatisticsResponse;
import org.serviceproject.statistics.dto.ServantStatisticsSummary;
import org.serviceproject.statistics.dto.StudentStatisticsResponse;
import org.serviceproject.statistics.dto.WeeklyTrendDataPoint;
import org.serviceproject.students.entity.StudentPlacement;
import org.serviceproject.students.entity.StudentStatus;
import org.serviceproject.students.repository.StudentPlacementRepository;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.repository.PersonRepository;
import org.serviceproject.users.repository.UserAccountRepository;
import org.serviceproject.visits.entity.VisitRecord;
import org.serviceproject.visits.repository.VisitRecordRepository;
import org.serviceproject.weeks.entity.Week;
import org.serviceproject.weeks.repository.WeekRepository;
import org.serviceproject.weeks.service.WeekService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service calculating dynamic statistics, KPIs, and consecutive absence alerts.
 * Never stores computed percentages in database tables.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final VisitRecordRepository visitRecordRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ConfessionRecordRepository confessionRecordRepository;
    private final StudentPlacementRepository studentPlacementRepository;
    private final StaffPlacementRepository staffPlacementRepository;
    private final MinistryRepository ministryRepository;
    private final GradeClassRepository gradeClassRepository;
    private final PersonRepository personRepository;
    private final UserAccountRepository userAccountRepository;
    private final WeekRepository weekRepository;
    private final WeekService weekService;
    private final AcademicYearService academicYearService;

    // Formula weights (0.40 visits, 0.25 mass, 0.25 meeting, 0.10 tasbeha)
    private static final double WEIGHT_VISIT = 0.40;
    private static final double WEIGHT_MASS = 0.25;
    private static final double WEIGHT_MEETING = 0.25;
    private static final double WEIGHT_TASBEHA = 0.10;

    @Transactional(readOnly = true)
    public DashboardStatisticsResponse getDashboard(Long weekId, UserPrincipal principal) {
        Week week = resolveWeek(weekId);
        AcademicYear currentYear = academicYearService.getCurrentEntity();
        List<StudentPlacement> students = getScopedStudents(principal, currentYear.getId());

        int absenceAlertsCount = calculateAbsenceAlertsCount(students, 2);

        return calculateDashboard(week, students, absenceAlertsCount);
    }

    @Transactional(readOnly = true)
    public MinistryStatisticsResponse getMinistryStatistics(Long ministryId, Long weekId, UserPrincipal principal) {
        checkMinistryScope(ministryId, principal);

        Ministry ministry = ministryRepository.findById(ministryId)
                .orElseThrow(() -> AppException.notFound("MINISTRY_NOT_FOUND", "الخدمة غير موجودة"));

        Week week = resolveWeek(weekId);
        AcademicYear currentYear = academicYearService.getCurrentEntity();

        List<StudentPlacement> ministryStudents = studentPlacementRepository.findAllByAcademicYearIdAndMinistryIdAndStatus(
                currentYear.getId(), ministryId, StudentStatus.ACTIVE);

        int absenceAlertsCount = calculateAbsenceAlertsCount(ministryStudents, 2);
        DashboardStatisticsResponse ministryDashboard = calculateDashboard(week, ministryStudents, absenceAlertsCount);

        List<GradeClass> classes = gradeClassRepository.findAllByMinistryIdAndActiveTrueOrderBySortOrderAsc(ministryId);
        List<ClassStatisticsSummary> classSummaries = new ArrayList<>();

        for (GradeClass gc : classes) {
            List<StudentPlacement> classStudents = ministryStudents.stream()
                    .filter(sp -> sp.getGradeClass() != null && sp.getGradeClass().getId().equals(gc.getId()))
                    .toList();

            DashboardStatisticsResponse classDashboard = calculateDashboard(week, classStudents, 0);

            classSummaries.add(new ClassStatisticsSummary(
                    gc.getId(),
                    gc.getName(),
                    classDashboard.totalStudents(),
                    classDashboard.visitedStudents(),
                    classDashboard.visitPercentage(),
                    classDashboard.meetingAttendanceCount(),
                    classDashboard.meetingAttendancePercentage(),
                    classDashboard.overallFollowupIndex()
            ));
        }

        return new MinistryStatisticsResponse(ministry.getId(), ministry.getName(), ministryDashboard, classSummaries);
    }

    @Transactional(readOnly = true)
    public ClassStatisticsResponse getClassStatistics(Long classId, Long weekId, UserPrincipal principal) {
        GradeClass gradeClass = gradeClassRepository.findByIdWithMinistry(classId)
                .orElseThrow(() -> AppException.notFound("CLASS_NOT_FOUND", "الفصل غير موجود"));

        checkClassScope(gradeClass, principal);

        Week week = resolveWeek(weekId);
        AcademicYear currentYear = academicYearService.getCurrentEntity();

        List<StudentPlacement> classStudents = studentPlacementRepository.findAllByAcademicYearIdAndClassIdAndStatus(
                currentYear.getId(), classId, StudentStatus.ACTIVE);

        int absenceAlertsCount = calculateAbsenceAlertsCount(classStudents, 2);
        DashboardStatisticsResponse classDashboard = calculateDashboard(week, classStudents, absenceAlertsCount);

        List<StaffPlacement> staffPlacements = staffPlacementRepository.findAllActiveByAcademicYearIdAndClassId(
                currentYear.getId(), classId);

        List<ServantStatisticsSummary> servantSummaries = new ArrayList<>();
        for (StaffPlacement sp : staffPlacements) {
            Person servant = sp.getPerson();
            List<StudentPlacement> servantStudents = classStudents.stream()
                    .filter(s -> s.getResponsibleServant() != null && s.getResponsibleServant().getId().equals(servant.getId()))
                    .toList();

            int assignedCount = servantStudents.size();
            List<VisitRecord> servantVisits = visitRecordRepository.findAllByWeekIdAndServantSnapId(
                    week.getId(), servant.getId());

            int visitedCount = servantVisits.size();
            double visitPercentage = safePercentage(visitedCount, assignedCount);

            servantSummaries.add(new ServantStatisticsSummary(
                    servant.getId(),
                    servant.getFullName(),
                    assignedCount,
                    visitedCount,
                    visitPercentage
            ));
        }

        return new ClassStatisticsResponse(
                gradeClass.getId(),
                gradeClass.getName(),
                gradeClass.getMinistry().getId(),
                gradeClass.getMinistry().getName(),
                classDashboard,
                servantSummaries
        );
    }

    @Transactional(readOnly = true)
    public ServantStatisticsResponse getServantStatistics(Long servantId, Long weekId, UserPrincipal principal) {
        Person servant = personRepository.findByIdAndDeletedAtIsNull(servantId)
                .orElseThrow(() -> AppException.notFound("SERVANT_NOT_FOUND", "الخادم غير موجود"));

        AcademicYear currentYear = academicYearService.getCurrentEntity();
        checkServantScope(servantId, currentYear.getId(), principal);

        Week week = resolveWeek(weekId);

        List<StudentPlacement> assignedStudents = studentPlacementRepository.findAllByAcademicYearIdAndServantIdAndStatus(
                currentYear.getId(), servantId, StudentStatus.ACTIVE);

        int assignedCount = assignedStudents.size();
        List<VisitRecord> visits = visitRecordRepository.findAllByWeekIdAndServantSnapId(week.getId(), servantId);

        int visitedCount = visits.size();
        double visitPercentage = safePercentage(visitedCount, assignedCount);

        Double avgPrayer = visits.stream()
                .filter(v -> v.getPrayerScore() != null)
                .mapToInt(VisitRecord::getPrayerScore)
                .average()
                .stream().map(v -> Math.round(v * 10.0) / 10.0).boxed()
                .findFirst().orElse(null);

        Double avgReading = visits.stream()
                .filter(v -> v.getReadingScore() != null)
                .mapToInt(VisitRecord::getReadingScore)
                .average()
                .stream().map(v -> Math.round(v * 10.0) / 10.0).boxed()
                .findFirst().orElse(null);

        Double avgNote = visits.stream()
                .filter(v -> v.getNoteScore() != null)
                .mapToInt(VisitRecord::getNoteScore)
                .average()
                .stream().map(v -> Math.round(v * 10.0) / 10.0).boxed()
                .findFirst().orElse(null);

        return new ServantStatisticsResponse(
                servant.getId(),
                servant.getFullName(),
                assignedCount,
                visitedCount,
                visitPercentage,
                avgPrayer,
                avgReading,
                avgNote
        );
    }

    @Transactional(readOnly = true)
    public List<WeeklyTrendDataPoint> getTrends(Integer count, UserPrincipal principal) {
        int limit = (count != null && count > 0) ? Math.min(count, 52) : 8;
        AcademicYear currentYear = academicYearService.getCurrentEntity();
        List<StudentPlacement> students = getScopedStudents(principal, currentYear.getId());

        List<Week> allWeeks = weekRepository.findAllByDeletedAtIsNullOrderByStartDateDesc();
        List<Week> targetWeeks = allWeeks.size() <= limit ? new ArrayList<>(allWeeks) : new ArrayList<>(allWeeks.subList(0, limit));
        Collections.reverse(targetWeeks); // Chronological order (oldest to newest)

        List<WeeklyTrendDataPoint> trends = new ArrayList<>();
        for (Week week : targetWeeks) {
            DashboardStatisticsResponse dash = calculateDashboard(week, students, 0);
            trends.add(new WeeklyTrendDataPoint(
                    week.getId(),
                    week.getStartDate().toString(),
                    week.getStartDate(),
                    week.getEndDate(),
                    dash.totalStudents(),
                    dash.visitedStudents(),
                    dash.visitPercentage(),
                    dash.massAttendanceCount(),
                    dash.massAttendancePercentage(),
                    dash.meetingAttendanceCount(),
                    dash.meetingAttendancePercentage(),
                    dash.tasbehaAttendanceCount(),
                    dash.tasbehaAttendancePercentage(),
                    dash.overallFollowupIndex()
            ));
        }
        return trends;
    }

    @Transactional(readOnly = true)
    public StudentStatisticsResponse getStudentStatistics(Long studentId, UserPrincipal principal) {
        Person student = personRepository.findByIdAndDeletedAtIsNull(studentId)
                .orElseThrow(() -> AppException.notFound("STUDENT_NOT_FOUND", "المخدوم غير موجود"));

        AcademicYear currentYear = academicYearService.getCurrentEntity();
        StudentPlacement placement = studentPlacementRepository.findByPersonIdAndAcademicYearId(studentId, currentYear.getId())
                .orElseThrow(() -> AppException.notFound("STUDENT_NOT_PLACED", "المخدوم غير مسكن في العام الأكاديمي الحالي"));

        if (!isStudentWithinScope(placement, principal)) {
            throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية لعرض إحصائيات هذا المخدوم");
        }

        List<Week> allWeeks = weekRepository.findAllByDeletedAtIsNullOrderByStartDateDesc();
        List<Week> currentYearWeeks = (currentYear != null && currentYear.getStartDate() != null && currentYear.getEndDate() != null)
                ? allWeeks.stream()
                        .filter(w -> !w.getStartDate().isBefore(currentYear.getStartDate()) && !w.getEndDate().isAfter(currentYear.getEndDate()))
                        .toList()
                : Collections.emptyList();
        int totalWeeks = currentYearWeeks.isEmpty() ? allWeeks.size() : currentYearWeeks.size();

        List<VisitRecord> visits = visitRecordRepository.findAllByStudentIdOrderByWeekDesc(studentId);
        int visitedWeeks = (int) visits.stream().map(v -> v.getWeek().getId()).distinct().count();
        double visitPercentage = safePercentage(visitedWeeks, totalWeeks);

        List<AttendanceRecord> attendanceRecords = attendanceRecordRepository.findAllPresentByStudentId(studentId);
        int massCount = (int) attendanceRecords.stream().filter(r -> r.getSession().getActivityType() == ActivityType.MASS).count();
        int meetingCount = (int) attendanceRecords.stream().filter(r -> r.getSession().getActivityType() == ActivityType.MEETING).count();
        int tasbehaCount = (int) attendanceRecords.stream().filter(r -> r.getSession().getActivityType() == ActivityType.TASBEHA).count();

        double massPercentage = safePercentage(massCount, totalWeeks);
        double meetingPercentage = safePercentage(meetingCount, totalWeeks);
        double tasbehaPercentage = safePercentage(tasbehaCount, totalWeeks);

        Double avgPrayer = visits.stream().filter(v -> v.getPrayerScore() != null).mapToInt(VisitRecord::getPrayerScore).average().stream().map(v -> Math.round(v * 10.0) / 10.0).boxed().findFirst().orElse(null);
        Double avgReading = visits.stream().filter(v -> v.getReadingScore() != null).mapToInt(VisitRecord::getReadingScore).average().stream().map(v -> Math.round(v * 10.0) / 10.0).boxed().findFirst().orElse(null);
        Double avgNote = visits.stream().filter(v -> v.getNoteScore() != null).mapToInt(VisitRecord::getNoteScore).average().stream().map(v -> Math.round(v * 10.0) / 10.0).boxed().findFirst().orElse(null);

        List<ConfessionRecord> confessions = confessionRecordRepository.findAllByStudentIdAndAcademicYearId(studentId, currentYear.getId());
        int totalConfessions = confessions.size();
        LocalDate lastConfessionDate = confessions.isEmpty() ? null : confessions.get(0).getConfessionDate();

        List<StudentStatisticsResponse.StudentRecentVisitSummary> recentVisits = visits.stream()
                .limit(5)
                .map(v -> new StudentStatisticsResponse.StudentRecentVisitSummary(
                        v.getId(),
                        v.getWeek().getId(),
                        v.getWeek().getStartDate(),
                        v.getMethod(),
                        v.getPrayerScore(),
                        v.getReadingScore(),
                        v.getNoteScore(),
                        v.getNotes(),
                        v.getServantSnap() != null ? v.getServantSnap().getFullName() : null,
                        v.getRecordedAt().toLocalDate()
                ))
                .toList();

        return new StudentStatisticsResponse(
                student.getId(),
                student.getFullName(),
                student.getPhone(),
                placement.getMinistry() != null ? placement.getMinistry().getName() : "",
                placement.getGradeClass() != null ? placement.getGradeClass().getName() : "",
                placement.getResponsibleServant() != null ? placement.getResponsibleServant().getFullName() : null,
                totalWeeks,
                visitedWeeks,
                visitPercentage,
                massCount,
                massPercentage,
                meetingCount,
                meetingPercentage,
                tasbehaCount,
                tasbehaPercentage,
                avgPrayer,
                avgReading,
                avgNote,
                totalConfessions,
                lastConfessionDate,
                recentVisits
        );
    }

    @Transactional(readOnly = true)
    public List<AbsenceAlertResponse> getAbsenceAlerts(UserPrincipal principal, int consecutiveWeeksThreshold) {
        int threshold = (consecutiveWeeksThreshold > 0) ? consecutiveWeeksThreshold : 2;
        AcademicYear currentYear = academicYearService.getCurrentEntity();
        List<StudentPlacement> students = getScopedStudents(principal, currentYear.getId());

        List<Week> pastWeeks = getRecentCompletedWeeks(threshold);
        if (pastWeeks.size() < threshold) {
            return Collections.emptyList();
        }

        List<Long> weekIds = pastWeeks.stream().map(Week::getId).toList();
        List<AbsenceAlertResponse> alerts = new ArrayList<>();

        for (StudentPlacement sp : students) {
            Person st = sp.getPerson();

            List<VisitRecord> studentVisits = visitRecordRepository.findAllByStudentIdAndWeekIds(st.getId(), weekIds);
            Set<Long> visitedWeekIds = new HashSet<>();
            for (VisitRecord vr : studentVisits) {
                visitedWeekIds.add(vr.getWeek().getId());
            }

            List<AttendanceRecord> meetings = attendanceRecordRepository.findAllPresentByStudentIdAndWeekIds(st.getId(), weekIds);
            Set<Long> meetingWeekIds = new HashSet<>();
            for (AttendanceRecord ar : meetings) {
                if (ar.getSession().getActivityType() == ActivityType.MEETING) {
                    meetingWeekIds.add(ar.getSession().getWeek().getId());
                }
            }

            boolean contactInAllWeeks = true;
            for (Long wid : weekIds) {
                if (!visitedWeekIds.contains(wid) && !meetingWeekIds.contains(wid)) {
                    // Missed this week
                } else {
                    contactInAllWeeks = false;
                    break;
                }
            }

            // If missed in all examined consecutive weeks -> alert!
            if (contactInAllWeeks) {
                LocalDate lastContactDate = null;
                if (!studentVisits.isEmpty()) {
                    lastContactDate = studentVisits.get(0).getRecordedAt().toLocalDate();
                }

                alerts.add(new AbsenceAlertResponse(
                        st.getId(),
                        st.getFullName(),
                        st.getPhone(),
                        sp.getGuardianPhone(),
                        sp.getMinistry() != null ? sp.getMinistry().getId() : null,
                        sp.getMinistry() != null ? sp.getMinistry().getName() : null,
                        sp.getGradeClass() != null ? sp.getGradeClass().getId() : null,
                        sp.getGradeClass() != null ? sp.getGradeClass().getName() : null,
                        sp.getResponsibleServant() != null ? sp.getResponsibleServant().getId() : null,
                        sp.getResponsibleServant() != null ? sp.getResponsibleServant().getFullName() : null,
                        threshold,
                        lastContactDate
                ));
            }
        }

        return alerts;
    }

    @Transactional(readOnly = true)
    public AdminSetupResponse getAdminSetupStatus(UserPrincipal principal) {
        if (!principal.isAdmin()) {
            throw AppException.forbidden("ACCESS_DENIED", "هذا الإجراء متاح فقط للأمين العام");
        }

        AcademicYear currentYear = academicYearService.getCurrentEntity();
        Long yearId = currentYear.getId();

        List<Ministry> ministries = ministryRepository.findAllByActiveTrue();
        List<GradeClass> classes = gradeClassRepository.findAllByActiveTrueOrderBySortOrderAsc();
        List<StaffPlacement> staffPlacements = staffPlacementRepository.findAllActiveByAcademicYearId(yearId);
        List<StudentPlacement> studentPlacements = studentPlacementRepository.findAllByAcademicYearIdAndStatus(yearId, StudentStatus.ACTIVE);

        int totalMinistries = ministries.size();
        int totalClasses = classes.size();
        int totalServants = staffPlacements.size();
        int totalStudents = studentPlacements.size();

        Set<Long> serviceSecMinistryIds = userAccountRepository.findAssignedServiceSecretaryMinistryIds();
        Set<Long> classSecClassIds = userAccountRepository.findAssignedClassSecretaryClassIds();

        int ministriesWithoutSecretary = (int) ministries.stream()
                .filter(m -> !serviceSecMinistryIds.contains(m.getId()))
                .count();

        int classesWithoutSecretary = (int) classes.stream()
                .filter(c -> !classSecClassIds.contains(c.getId()))
                .count();

        Set<Long> servantPersonIds = staffPlacements.stream()
                .map(sp -> sp.getPerson().getId())
                .collect(java.util.stream.Collectors.toSet());

        Set<Long> servantsWithAccountIds = servantPersonIds.isEmpty() ? Collections.emptySet()
                : userAccountRepository.findPersonIdsWithEnabledAccount(servantPersonIds);

        int servantsWithAccount = servantsWithAccountIds.size();
        int servantsWithoutAccount = totalServants - servantsWithAccount;

        int studentsWithoutServant = (int) studentPlacements.stream()
                .filter(sp -> sp.getResponsibleServant() == null)
                .count();

        boolean hasMinistry = totalMinistries > 0;
        boolean hasClasses = totalClasses > 0;
        boolean hasServiceSecretary = hasMinistry && ministriesWithoutSecretary < totalMinistries;
        boolean hasClassSecretary = hasClasses && classesWithoutSecretary < totalClasses;
        boolean hasServants = totalServants > 0;
        boolean hasStudents = totalStudents > 0;
        boolean hasAssignments = totalStudents > 0 && studentsWithoutServant < totalStudents;

        boolean setupComplete = hasMinistry && hasClasses && ministriesWithoutSecretary == 0
                && classesWithoutSecretary == 0 && hasServants && hasStudents && studentsWithoutServant == 0
                && servantsWithoutAccount == 0;

        List<AdminSetupResponse.ConfigurationWarning> warnings = new ArrayList<>();

        if (totalMinistries == 0) {
            warnings.add(new AdminSetupResponse.ConfigurationWarning(
                    "NO_MINISTRIES",
                    "لا توجد خدمات مسجلة حتى الآن",
                    "قم بإنشاء أول خدمة (مثل: ابتدائي، إعدادي) للبدء في تنظيم الخدمة.",
                    "WARNING",
                    "إنشاء خدمة جديدة",
                    "/ministries",
                    1
            ));
        } else if (ministriesWithoutSecretary > 0) {
            warnings.add(new AdminSetupResponse.ConfigurationWarning(
                    "MINISTRY_WITHOUT_SECRETARY",
                    "يوجد خدمات بدون أمين خدمة",
                    "يوجد " + ministriesWithoutSecretary + " خدمة ليس لها أمين خدمة معين مسؤول عنها.",
                    "WARNING",
                    "تعيين أمين خدمة",
                    "/ministries",
                    ministriesWithoutSecretary
            ));
        }

        if (totalClasses == 0 && totalMinistries > 0) {
            warnings.add(new AdminSetupResponse.ConfigurationWarning(
                    "NO_CLASSES",
                    "لا توجد فصول دراسية",
                    "قم بإضافة فصول دراسية للخدمات الحالية لتوزيع المخدومين والخدام.",
                    "WARNING",
                    "إضافة فصل",
                    "/ministries",
                    1
            ));
        } else if (classesWithoutSecretary > 0) {
            warnings.add(new AdminSetupResponse.ConfigurationWarning(
                    "CLASS_WITHOUT_SECRETARY",
                    "يوجد فصول بدون أمين فصل",
                    "يوجد " + classesWithoutSecretary + " فصل ليس له أمين فصل معين.",
                    "WARNING",
                    "تعيين أمين فصل",
                    "/ministries",
                    classesWithoutSecretary
            ));
        }

        if (servantsWithoutAccount > 0) {
            warnings.add(new AdminSetupResponse.ConfigurationWarning(
                    "SERVANTS_WITHOUT_ACCOUNT",
                    "خدام بدون حسابات دخول",
                    "يوجد " + servantsWithoutAccount + " خادم مسكن بالخدمة لا يملك حساباً لتسجيل الدخول والافتقاد.",
                    "WARNING",
                    "إدارة المستخدمين",
                    "/users",
                    servantsWithoutAccount
            ));
        }

        if (studentsWithoutServant > 0) {
            warnings.add(new AdminSetupResponse.ConfigurationWarning(
                    "STUDENTS_WITHOUT_SERVANT",
                    "مخدومين بدون خادم مسؤول",
                    "يوجد " + studentsWithoutServant + " مخدوم مسجل بدون خادم مسؤول عن متابعته وافتقاده.",
                    "WARNING",
                    "توزيع المخدومين",
                    "/students",
                    studentsWithoutServant
            ));
        }

        return new AdminSetupResponse(
                totalMinistries,
                totalClasses,
                totalServants,
                totalStudents,
                servantsWithAccount,
                servantsWithoutAccount,
                ministriesWithoutSecretary,
                classesWithoutSecretary,
                studentsWithoutServant,
                hasMinistry,
                hasClasses,
                hasServiceSecretary,
                hasClassSecretary,
                hasServants,
                hasStudents,
                hasAssignments,
                setupComplete,
                warnings
        );
    }

    // ── Internal Helpers ─────────────────────────────────────────────

    private DashboardStatisticsResponse calculateDashboard(Week week, List<StudentPlacement> students, int absenceAlertsCount) {
        int total = students.size();
        Set<Long> studentIds = new HashSet<>();
        for (StudentPlacement sp : students) {
            studentIds.add(sp.getPerson().getId());
        }

        // Visited count in week
        List<VisitRecord> weekVisits = visitRecordRepository.findAllByWeekId(week.getId());
        int visitedCount = (int) weekVisits.stream()
                .filter(v -> studentIds.contains(v.getStudent().getId()))
                .map(v -> v.getStudent().getId())
                .distinct()
                .count();

        double visitPercentage = safePercentage(visitedCount, total);

        // Mass attendance
        List<AttendanceRecord> massRecords = attendanceRecordRepository.findAllPresentByWeekIdAndActivityType(
                week.getId(), ActivityType.MASS);
        int massCount = (int) massRecords.stream()
                .filter(r -> studentIds.contains(r.getStudent().getId()))
                .map(r -> r.getStudent().getId())
                .distinct()
                .count();
        double massPercentage = safePercentage(massCount, total);

        // Meeting attendance
        List<AttendanceRecord> meetingRecords = attendanceRecordRepository.findAllPresentByWeekIdAndActivityType(
                week.getId(), ActivityType.MEETING);
        int meetingCount = (int) meetingRecords.stream()
                .filter(r -> studentIds.contains(r.getStudent().getId()))
                .map(r -> r.getStudent().getId())
                .distinct()
                .count();
        double meetingPercentage = safePercentage(meetingCount, total);

        // Tasbeha attendance
        List<AttendanceRecord> tasbehaRecords = attendanceRecordRepository.findAllPresentByWeekIdAndActivityType(
                week.getId(), ActivityType.TASBEHA);
        int tasbehaCount = (int) tasbehaRecords.stream()
                .filter(r -> studentIds.contains(r.getStudent().getId()))
                .map(r -> r.getStudent().getId())
                .distinct()
                .count();
        double tasbehaPercentage = safePercentage(tasbehaCount, total);

        // Overall follow-up index
        double overallIndex = roundOneDecimal(
                (WEIGHT_VISIT * visitPercentage) +
                (WEIGHT_MASS * massPercentage) +
                (WEIGHT_MEETING * meetingPercentage) +
                (WEIGHT_TASBEHA * tasbehaPercentage)
        );

        return new DashboardStatisticsResponse(
                week.getId(),
                week.getStartDate(),
                week.getEndDate(),
                total,
                visitedCount,
                visitPercentage,
                massCount,
                massPercentage,
                meetingCount,
                meetingPercentage,
                tasbehaCount,
                tasbehaPercentage,
                overallIndex,
                absenceAlertsCount
        );
    }

    private double safePercentage(int numerator, int denominator) {
        if (denominator <= 0) return 0.0;
        return roundOneDecimal(numerator * 100.0 / denominator);
    }

    private double roundOneDecimal(double val) {
        return Math.round(val * 10.0) / 10.0;
    }

    private Week resolveWeek(Long weekId) {
        if (weekId != null) {
            return weekService.getWeekOrThrow(weekId);
        }
        return weekService.getCurrentWeekEntity();
    }

    private List<StudentPlacement> getScopedStudents(UserPrincipal principal, Long currentYearId) {
        if (principal.isAdmin()) {
            return studentPlacementRepository.findAllByAcademicYearIdAndStatus(currentYearId, StudentStatus.ACTIVE);
        }
        if (principal.isServiceSecretary()) {
            return studentPlacementRepository.findAllByAcademicYearIdAndMinistryIdAndStatus(
                    currentYearId, principal.getServiceSecretaryMinistryId(), StudentStatus.ACTIVE);
        }
        if (principal.isClassSecretary()) {
            return studentPlacementRepository.findAllByAcademicYearIdAndClassIdAndStatus(
                    currentYearId, principal.getClassSecretaryClassId(), StudentStatus.ACTIVE);
        }
        if (principal.isServant()) {
            return studentPlacementRepository.findAllByAcademicYearIdAndServantIdAndStatus(
                    currentYearId, principal.getPersonId(), StudentStatus.ACTIVE);
        }
        return Collections.emptyList();
    }

    private int calculateAbsenceAlertsCount(List<StudentPlacement> students, int threshold) {
        List<Week> pastWeeks = getRecentCompletedWeeks(threshold);
        if (pastWeeks.size() < threshold || students.isEmpty()) {
            return 0;
        }

        List<Long> weekIds = pastWeeks.stream().map(Week::getId).toList();
        int count = 0;

        for (StudentPlacement sp : students) {
            Long sid = sp.getPerson().getId();
            List<VisitRecord> studentVisits = visitRecordRepository.findAllByStudentIdAndWeekIds(sid, weekIds);
            List<AttendanceRecord> meetings = attendanceRecordRepository.findAllPresentByStudentIdAndWeekIds(sid, weekIds);

            Set<Long> visitedWeekIds = new HashSet<>();
            for (VisitRecord vr : studentVisits) visitedWeekIds.add(vr.getWeek().getId());

            Set<Long> meetingWeekIds = new HashSet<>();
            for (AttendanceRecord ar : meetings) {
                if (ar.getSession().getActivityType() == ActivityType.MEETING) {
                    meetingWeekIds.add(ar.getSession().getWeek().getId());
                }
            }

            boolean absentAllWeeks = true;
            for (Long wid : weekIds) {
                if (visitedWeekIds.contains(wid) || meetingWeekIds.contains(wid)) {
                    absentAllWeeks = false;
                    break;
                }
            }
            if (absentAllWeeks) {
                count++;
            }
        }

        return count;
    }

    private List<Week> getRecentCompletedWeeks(int count) {
        Week currentWeek = null;
        try {
            currentWeek = weekService.getCurrentWeekEntity();
        } catch (Exception ignored) {}
        final Long currentWeekId = currentWeek != null ? currentWeek.getId() : null;

        List<Week> allWeeks = weekRepository.findAllByDeletedAtIsNullOrderByStartDateDesc();
        List<Week> completedWeeks = allWeeks.stream()
                .filter(w -> currentWeekId == null || !w.getId().equals(currentWeekId))
                .toList();

        if (completedWeeks.size() <= count) {
            return completedWeeks;
        }
        return completedWeeks.subList(0, count);
    }

    private void checkMinistryScope(Long ministryId, UserPrincipal principal) {
        if (principal.isAdmin()) return;
        if (principal.isServiceSecretary() && principal.getServiceSecretaryMinistryId().equals(ministryId)) return;
        throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية لعرض إحصائيات هذه الخدمة");
    }

    private void checkClassScope(GradeClass gradeClass, UserPrincipal principal) {
        if (principal.isAdmin()) return;
        if (principal.isServiceSecretary() && gradeClass.getMinistry().getId().equals(principal.getServiceSecretaryMinistryId())) return;
        if (principal.isClassSecretary() && gradeClass.getId().equals(principal.getClassSecretaryClassId())) return;
        throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية لعرض إحصائيات هذا الفصل");
    }

    private void checkServantScope(Long servantId, Long currentYearId, UserPrincipal principal) {
        if (principal.isAdmin()) return;
        if (principal.isServant() && principal.getPersonId().equals(servantId)) return;

        StaffPlacement sp = staffPlacementRepository.findByPersonIdAndAcademicYearId(servantId, currentYearId)
                .orElseThrow(() -> AppException.notFound("SERVANT_NOT_PLACED", "الخادم غير مسكن"));

        if (principal.isServiceSecretary() && sp.getMinistry().getId().equals(principal.getServiceSecretaryMinistryId())) return;
        if (principal.isClassSecretary() && sp.getGradeClass().getId().equals(principal.getClassSecretaryClassId())) return;

        throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية لعرض إحصائيات هذا الخادم");
    }

    private boolean isStudentWithinScope(StudentPlacement sp, UserPrincipal principal) {
        if (principal == null) return false;
        if (principal.isAdmin()) return true;
        if (principal.isServiceSecretary()) {
            return sp.getMinistry() != null && sp.getMinistry().getId().equals(principal.getServiceSecretaryMinistryId());
        }
        if (principal.isClassSecretary()) {
            return sp.getGradeClass() != null && sp.getGradeClass().getId().equals(principal.getClassSecretaryClassId());
        }
        if (principal.isServant()) {
            return sp.getResponsibleServant() != null && sp.getResponsibleServant().getId().equals(principal.getPersonId());
        }
        return false;
    }
}
