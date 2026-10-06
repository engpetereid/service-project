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
import org.serviceproject.statistics.dto.ServantPerformanceResponse;
import org.serviceproject.statistics.dto.ServantStatisticsResponse;
import org.serviceproject.statistics.dto.ServantStatisticsSummary;
import org.serviceproject.statistics.dto.StudentStatisticsResponse;
import org.serviceproject.statistics.dto.WeeklyTrendDataPoint;
import org.serviceproject.students.entity.StudentPlacement;
import org.serviceproject.students.entity.StudentStatus;
import org.serviceproject.students.repository.StudentPlacementRepository;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.repository.PersonRepository;
import org.serviceproject.users.repository.UserAccountRepository;
import org.serviceproject.selffollowup.entity.ServantWeeklyFollowUp;
import org.serviceproject.selffollowup.repository.ServantWeeklyFollowUpRepository;
import org.serviceproject.visits.entity.VisitRecord;
import org.serviceproject.visits.repository.VisitRecordRepository;
import org.serviceproject.weeks.entity.Week;
import org.serviceproject.weeks.repository.WeekRepository;
import org.serviceproject.weeks.service.WeekService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final ServantWeeklyFollowUpRepository servantWeeklyFollowUpRepository;
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

        List<Long> personIds = staffPlacements.stream().map(sp -> sp.getPerson().getId()).toList();
        Map<Long, UserAccount> userAccountByPersonId = userAccountRepository.findAllByPersonIdIn(personIds).stream()
                .collect(Collectors.toMap(u -> u.getPerson().getId(), u -> u, (a, b) -> a));
        List<Long> userIds = userAccountByPersonId.values().stream().map(UserAccount::getId).toList();

        Map<Long, ServantWeeklyFollowUp> followUpByUserId = Collections.emptyMap();
        if (servantWeeklyFollowUpRepository != null && !userIds.isEmpty()) {
            followUpByUserId = servantWeeklyFollowUpRepository.findAllByWeekIdAndUserIdIn(week.getId(), userIds).stream()
                    .collect(Collectors.toMap(f -> f.getUser().getId(), f -> f, (a, b) -> a));
        }

        List<ServantStatisticsSummary> servantSummaries = new ArrayList<>();
        for (StaffPlacement sp : staffPlacements) {
            Person servant = sp.getPerson();
            UserAccount ua = userAccountByPersonId.get(servant.getId());
            ServantWeeklyFollowUp fu = (ua != null) ? followUpByUserId.get(ua.getId()) : null;

            List<StudentPlacement> servantStudents = classStudents.stream()
                    .filter(s -> s.getResponsibleServant() != null && s.getResponsibleServant().getId().equals(servant.getId()))
                    .toList();

            int assignedCount = servantStudents.size();
            List<VisitRecord> servantVisits = visitRecordRepository.findAllByWeekIdAndServantSnapId(
                    week.getId(), servant.getId());

            int visitedCount = servantVisits.size();
            double visitPercentage = safePercentage(visitedCount, assignedCount);

            boolean recorded = (fu != null);
            Integer noteScore = fu != null ? fu.getNoteScore() : null;
            Integer maxNote = fu != null ? fu.getMaxNoteScoreSnapshot() : null;
            Double notePct = (noteScore != null && maxNote != null && maxNote > 0)
                    ? Math.round((noteScore * 100.0 / maxNote) * 10.0) / 10.0 : null;
            Boolean attendedMeeting = fu != null ? fu.getAttendedServiceMeeting() : null;
            Boolean attendedMass = fu != null ? fu.getAttendedMass() : null;
            Boolean attendedTasbeha = fu != null ? fu.getAttendedTasbeha() : null;
            Boolean attendedManagement = fu != null ? fu.getAttendedManagementMeeting() : null;

            servantSummaries.add(new ServantStatisticsSummary(
                    servant.getId(),
                    servant.getFullName(),
                    gradeClass.getMinistry().getId(),
                    gradeClass.getMinistry().getName(),
                    gradeClass.getId(),
                    gradeClass.getName(),
                    assignedCount,
                    visitedCount,
                    visitPercentage,
                    noteScore,
                    maxNote,
                    notePct,
                    attendedMeeting,
                    attendedMass,
                    attendedTasbeha,
                    attendedManagement,
                    recorded
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

        StaffPlacement sp = staffPlacementRepository.findByPersonIdAndAcademicYearId(servantId, currentYear.getId()).orElse(null);
        Long ministryId = (sp != null && sp.getMinistry() != null) ? sp.getMinistry().getId() : null;
        String ministryName = (sp != null && sp.getMinistry() != null) ? sp.getMinistry().getName() : null;
        Long classId = (sp != null && sp.getGradeClass() != null) ? sp.getGradeClass().getId() : null;
        String className = (sp != null && sp.getGradeClass() != null) ? sp.getGradeClass().getName() : null;

        UserAccount ua = userAccountRepository.findByPersonId(servantId).orElse(null);
        ServantWeeklyFollowUp fu = (ua != null && servantWeeklyFollowUpRepository != null)
                ? servantWeeklyFollowUpRepository.findByUserIdAndWeekId(ua.getId(), week.getId()).orElse(null)
                : null;

        Integer servantNote = fu != null ? fu.getNoteScore() : null;
        Integer servantMaxNote = fu != null ? fu.getMaxNoteScoreSnapshot() : null;
        Double servantNotePct = (servantNote != null && servantMaxNote != null && servantMaxNote > 0)
                ? Math.round((servantNote * 100.0 / servantMaxNote) * 10.0) / 10.0 : null;
        Boolean servantMass = fu != null ? fu.getAttendedMass() : null;
        Boolean servantMeeting = fu != null ? fu.getAttendedServiceMeeting() : null;
        Boolean servantTasbeha = fu != null ? fu.getAttendedTasbeha() : null;
        Boolean servantManagement = fu != null ? fu.getAttendedManagementMeeting() : null;
        boolean recordedFollowUp = (fu != null);

        Double annualRecordingRate = null;
        Double annualAvgNotePct = null;
        Double annualMassRate = null;
        Double annualMeetingRate = null;
        Double annualVisitPct = null;
        List<ServantStatisticsResponse.ServantRecentWeekRecord> recentWeeks = new ArrayList<>();

        if (ua != null && servantWeeklyFollowUpRepository != null) {
            List<ServantWeeklyFollowUp> yearFollowUps = servantWeeklyFollowUpRepository
                    .findAllByUserIdAndAcademicYearIdOrderByWeekStartDateDesc(ua.getId(), currentYear.getId());

            List<Week> allYearWeeks = weekRepository.findAllByDeletedAtIsNullOrderByStartDateDesc().stream()
                    .filter(w -> !w.getStartDate().isBefore(currentYear.getStartDate()) && !w.getEndDate().isAfter(currentYear.getEndDate()))
                    .toList();

            long totalPassedWeeks = allYearWeeks.stream()
                    .filter(w -> !w.getStartDate().isAfter(week.getStartDate()))
                    .count();
            if (totalPassedWeeks <= 0) totalPassedWeeks = Math.max(1, yearFollowUps.size());

            annualRecordingRate = safePercentage(yearFollowUps.size(), (int) totalPassedWeeks);

            annualAvgNotePct = yearFollowUps.stream()
                    .filter(f -> f.getNoteScore() != null && f.getMaxNoteScoreSnapshot() != null && f.getMaxNoteScoreSnapshot() > 0)
                    .mapToDouble(f -> f.getNoteScore() * 100.0 / f.getMaxNoteScoreSnapshot())
                    .average()
                    .stream().map(v -> Math.round(v * 10.0) / 10.0).boxed()
                    .findFirst().orElse(null);

            annualMassRate = yearFollowUps.stream()
                    .filter(f -> f.getAttendedMass() != null)
                    .mapToDouble(f -> Boolean.TRUE.equals(f.getAttendedMass()) ? 100.0 : 0.0)
                    .average()
                    .stream().map(v -> Math.round(v * 10.0) / 10.0).boxed()
                    .findFirst().orElse(null);

            annualMeetingRate = yearFollowUps.stream()
                    .filter(f -> f.getAttendedServiceMeeting() != null)
                    .mapToDouble(f -> Boolean.TRUE.equals(f.getAttendedServiceMeeting()) ? 100.0 : 0.0)
                    .average()
                    .stream().map(v -> Math.round(v * 10.0) / 10.0).boxed()
                    .findFirst().orElse(null);

            List<Week> targetWeeks = allYearWeeks.stream()
                    .filter(w -> !w.getStartDate().isAfter(week.getStartDate()))
                    .limit(8)
                    .toList();

            Map<Long, ServantWeeklyFollowUp> followUpByWeekId = yearFollowUps.stream()
                    .collect(Collectors.toMap(f -> f.getWeek().getId(), f -> f, (a, b) -> a));

            int totalYearVisits = 0;
            for (Week pw : targetWeeks) {
                ServantWeeklyFollowUp pfu = followUpByWeekId.get(pw.getId());
                List<VisitRecord> pVisits = visitRecordRepository.findAllByWeekIdAndServantSnapId(pw.getId(), servantId);
                int pVisited = pVisits.size();
                double pVisitPct = safePercentage(pVisited, assignedCount);

                Integer pNote = pfu != null ? pfu.getNoteScore() : null;
                Integer pMaxNote = pfu != null ? pfu.getMaxNoteScoreSnapshot() : null;
                Double pNotePct = (pNote != null && pMaxNote != null && pMaxNote > 0)
                        ? Math.round((pNote * 100.0 / pMaxNote) * 10.0) / 10.0 : null;

                recentWeeks.add(new ServantStatisticsResponse.ServantRecentWeekRecord(
                        pw.getId(),
                        pw.getStartDate(),
                        pw.getEndDate(),
                        pNote,
                        pMaxNote,
                        pNotePct,
                        pfu != null ? pfu.getAttendedMass() : null,
                        pfu != null ? pfu.getAttendedServiceMeeting() : null,
                        pVisited,
                        assignedCount,
                        pVisitPct,
                        pfu != null
                ));
            }

            if (assignedCount > 0 && totalPassedWeeks > 0) {
                for (Week yw : allYearWeeks) {
                    if (!yw.getStartDate().isAfter(week.getStartDate())) {
                        totalYearVisits += visitRecordRepository.findAllByWeekIdAndServantSnapId(yw.getId(), servantId).size();
                    }
                }
                annualVisitPct = safePercentage(totalYearVisits, (int) (assignedCount * totalPassedWeeks));
            }
        }

        return new ServantStatisticsResponse(
                servant.getId(),
                servant.getFullName(),
                ministryId,
                ministryName,
                classId,
                className,
                servant.getPhone(),
                assignedCount,
                visitedCount,
                visitPercentage,
                servantNote,
                servantMaxNote,
                servantNotePct,
                servantMass,
                servantMeeting,
                servantTasbeha,
                servantManagement,
                recordedFollowUp,
                avgPrayer,
                avgReading,
                avgNote,
                annualRecordingRate,
                annualAvgNotePct,
                annualMassRate,
                annualMeetingRate,
                annualVisitPct,
                recentWeeks
        );
    }

    @Transactional(readOnly = true)
    public ServantPerformanceResponse getServantsPerformance(Long ministryId, Long classId, Long weekId, UserPrincipal principal) {
        if (principal == null) {
            throw AppException.unauthorized("UNAUTHORIZED", "غير مصرح");
        }
        if (!principal.isAdmin() && !principal.isServiceSecretary() && !principal.isClassSecretary()) {
            throw AppException.forbidden("ACCESS_DENIED", "هذه الصفحة مخصصة للمسؤولين وأمناء الخدمة والفصول فقط");
        }

        if (principal.isServiceSecretary() && !principal.isAdmin()) {
            Long managedMinistryId = principal.getServiceSecretaryMinistryId();
            if (ministryId != null && !ministryId.equals(managedMinistryId)) {
                throw AppException.forbidden("ACCESS_DENIED", "لا تملك صلاحية للاطلاع على إحصائيات هذه الخدمة");
            }
            ministryId = managedMinistryId;
        } else if (principal.isClassSecretary() && !principal.isAdmin()) {
            Long managedClassId = principal.getClassSecretaryClassId();
            if (classId != null && !classId.equals(managedClassId)) {
                throw AppException.forbidden("ACCESS_DENIED", "لا تملك صلاحية للاطلاع على إحصائيات هذا الفصل");
            }
            classId = managedClassId;
        }

        if (classId != null) {
            GradeClass gc = gradeClassRepository.findByIdWithMinistry(classId)
                    .orElseThrow(() -> AppException.notFound("CLASS_NOT_FOUND", "الفصل غير موجود"));
            checkClassScope(gc, principal);
        } else if (ministryId != null) {
            Ministry m = ministryRepository.findById(ministryId)
                    .orElseThrow(() -> AppException.notFound("MINISTRY_NOT_FOUND", "الخدمة غير موجودة"));
            checkMinistryScope(ministryId, principal);
        }

        Week week = resolveWeek(weekId);
        AcademicYear currentYear = academicYearService.getCurrentEntity();

        List<StaffPlacement> staffPlacements;
        if (classId != null) {
            staffPlacements = staffPlacementRepository.findAllActiveByAcademicYearIdAndClassId(currentYear.getId(), classId);
        } else if (ministryId != null) {
            staffPlacements = staffPlacementRepository.findAllActiveByAcademicYearIdAndMinistryId(currentYear.getId(), ministryId);
        } else {
            staffPlacements = staffPlacementRepository.findAllActiveByAcademicYearId(currentYear.getId());
        }

        if (staffPlacements.isEmpty()) {
            return new ServantPerformanceResponse(0, 0, 0.0, null, null, null, 0.0, Collections.emptyList());
        }

        List<Long> personIds = staffPlacements.stream()
                .map(sp -> sp.getPerson().getId())
                .distinct()
                .toList();

        Map<Long, UserAccount> userAccountByPersonId = userAccountRepository.findAllByPersonIdIn(personIds).stream()
                .collect(Collectors.toMap(u -> u.getPerson().getId(), u -> u, (a, b) -> a));

        List<Long> userIds = userAccountByPersonId.values().stream()
                .map(UserAccount::getId)
                .toList();

        Map<Long, ServantWeeklyFollowUp> followUpByUserId = Collections.emptyMap();
        if (servantWeeklyFollowUpRepository != null && !userIds.isEmpty()) {
            followUpByUserId = servantWeeklyFollowUpRepository.findAllByWeekIdAndUserIdIn(week.getId(), userIds).stream()
                    .collect(Collectors.toMap(f -> f.getUser().getId(), f -> f, (a, b) -> a));
        }

        List<StudentPlacement> students;
        if (classId != null) {
            students = studentPlacementRepository.findAllByAcademicYearIdAndClassIdAndStatus(currentYear.getId(), classId, StudentStatus.ACTIVE);
        } else if (ministryId != null) {
            students = studentPlacementRepository.findAllByAcademicYearIdAndMinistryIdAndStatus(currentYear.getId(), ministryId, StudentStatus.ACTIVE);
        } else {
            students = studentPlacementRepository.findAllByAcademicYearIdAndStatus(currentYear.getId(), StudentStatus.ACTIVE);
        }

        Map<Long, List<StudentPlacement>> assignedStudentsByServantId = students.stream()
                .filter(s -> s.getResponsibleServant() != null)
                .collect(Collectors.groupingBy(s -> s.getResponsibleServant().getId()));

        List<VisitRecord> allVisits = visitRecordRepository.findAllByWeekId(week.getId());
        Map<Long, List<VisitRecord>> visitsByServantId = allVisits.stream()
                .filter(v -> v.getServantSnap() != null)
                .collect(Collectors.groupingBy(v -> v.getServantSnap().getId()));

        List<ServantStatisticsSummary> summaries = new ArrayList<>();
        int totalAssignedAll = 0;
        int totalVisitedAll = 0;

        for (StaffPlacement sp : staffPlacements) {
            Person servant = sp.getPerson();
            UserAccount ua = userAccountByPersonId.get(servant.getId());
            ServantWeeklyFollowUp fu = (ua != null) ? followUpByUserId.get(ua.getId()) : null;

            List<StudentPlacement> myStudents = assignedStudentsByServantId.getOrDefault(servant.getId(), Collections.emptyList());
            int assignedCount = myStudents.size();
            List<VisitRecord> myVisits = visitsByServantId.getOrDefault(servant.getId(), Collections.emptyList());
            int visitedCount = myVisits.size();
            double visitPct = safePercentage(visitedCount, assignedCount);

            totalAssignedAll += assignedCount;
            totalVisitedAll += visitedCount;

            boolean recorded = (fu != null);
            Integer noteScore = fu != null ? fu.getNoteScore() : null;
            Integer maxNote = fu != null ? fu.getMaxNoteScoreSnapshot() : null;
            Double notePct = (noteScore != null && maxNote != null && maxNote > 0)
                    ? Math.round((noteScore * 100.0 / maxNote) * 10.0) / 10.0 : null;
            Boolean attendedMeeting = fu != null ? fu.getAttendedServiceMeeting() : null;
            Boolean attendedMass = fu != null ? fu.getAttendedMass() : null;
            Boolean attendedTasbeha = fu != null ? fu.getAttendedTasbeha() : null;
            Boolean attendedManagement = fu != null ? fu.getAttendedManagementMeeting() : null;

            summaries.add(new ServantStatisticsSummary(
                    servant.getId(),
                    servant.getFullName(),
                    sp.getMinistry() != null ? sp.getMinistry().getId() : null,
                    sp.getMinistry() != null ? sp.getMinistry().getName() : null,
                    sp.getGradeClass() != null ? sp.getGradeClass().getId() : null,
                    sp.getGradeClass() != null ? sp.getGradeClass().getName() : null,
                    assignedCount,
                    visitedCount,
                    visitPct,
                    noteScore,
                    maxNote,
                    notePct,
                    attendedMeeting,
                    attendedMass,
                    attendedTasbeha,
                    attendedManagement,
                    recorded
            ));
        }

        int totalServants = summaries.size();
        int recordedCount = (int) summaries.stream().filter(ServantStatisticsSummary::recordedSelfFollowUp).count();
        double submissionRate = safePercentage(recordedCount, totalServants);

        Double avgNotePercentage = summaries.stream()
                .map(ServantStatisticsSummary::notePercentage)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .stream().map(v -> Math.round(v * 10.0) / 10.0).boxed()
                .findFirst().orElse(null);

        Double massAttendanceRate = null;
        Double meetingAttendanceRate = null;
        if (recordedCount > 0) {
            long massAttended = summaries.stream()
                    .filter(s -> Boolean.TRUE.equals(s.attendedMass()))
                    .count();
            massAttendanceRate = Math.round((massAttended * 100.0 / recordedCount) * 10.0) / 10.0;

            long meetingAttended = summaries.stream()
                    .filter(s -> Boolean.TRUE.equals(s.attendedServiceMeeting()))
                    .count();
            meetingAttendanceRate = Math.round((meetingAttended * 100.0 / recordedCount) * 10.0) / 10.0;
        }

        double overallVisitPercentage = safePercentage(totalVisitedAll, totalAssignedAll);

        return new ServantPerformanceResponse(
                totalServants,
                recordedCount,
                submissionRate,
                avgNotePercentage,
                massAttendanceRate,
                meetingAttendanceRate,
                overallVisitPercentage,
                summaries
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
