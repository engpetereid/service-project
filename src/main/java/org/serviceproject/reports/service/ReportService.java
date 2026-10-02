package org.serviceproject.reports.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.service.AcademicYearService;
import org.serviceproject.attendance.entity.ActivityType;
import org.serviceproject.attendance.entity.AttendanceRecord;
import org.serviceproject.attendance.entity.AttendanceSession;
import org.serviceproject.attendance.repository.AttendanceRecordRepository;
import org.serviceproject.attendance.repository.AttendanceSessionRepository;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.common.util.CsvUtil;
import org.serviceproject.confession.entity.ConfessionRecord;
import org.serviceproject.confession.repository.ConfessionRecordRepository;
import org.serviceproject.reports.dto.AttendanceReportFilter;
import org.serviceproject.reports.dto.ConfessionReportFilter;
import org.serviceproject.reports.dto.StudentReportFilter;
import org.serviceproject.reports.dto.VisitReportFilter;
import org.serviceproject.students.entity.StudentPlacement;
import org.serviceproject.students.entity.StudentStatus;
import org.serviceproject.students.repository.StudentPlacementRepository;
import org.serviceproject.users.entity.Gender;
import org.serviceproject.users.entity.Person;
import org.serviceproject.visits.entity.VisitMethod;
import org.serviceproject.visits.entity.VisitRecord;
import org.serviceproject.visits.repository.VisitRecordRepository;
import org.serviceproject.weeks.entity.Week;
import org.serviceproject.weeks.service.WeekService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service generating Arabic CSV exports for students, follow-up visits, attendance, and confessions
 * with role-based scope enforcement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final StudentPlacementRepository studentPlacementRepository;
    private final VisitRecordRepository visitRecordRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final ConfessionRecordRepository confessionRecordRepository;
    private final AcademicYearService academicYearService;
    private final WeekService weekService;

    /**
     * Exports student roster to CSV with role-scoped filtering.
     */
    @Transactional(readOnly = true)
    public byte[] exportStudentsCsv(StudentReportFilter filter, UserPrincipal principal) {
        Long requestedMinistryId = filter != null ? filter.ministryId() : null;
        Long requestedClassId = filter != null ? filter.classId() : null;
        Long requestedServantId = filter != null ? filter.servantId() : null;
        StudentStatus requestedStatus = filter != null ? filter.status() : null;

        checkExplicitScope(requestedMinistryId, requestedClassId, requestedServantId, principal);

        Long yearId = (filter != null && filter.academicYearId() != null)
                ? filter.academicYearId()
                : academicYearService.getCurrentEntity().getId();

        List<StudentPlacement> placements = (requestedStatus != null)
                ? studentPlacementRepository.findAllByAcademicYearIdAndStatus(yearId, requestedStatus)
                : studentPlacementRepository.findAllByAcademicYearId(yearId);

        List<StudentPlacement> scoped = placements.stream()
                .filter(sp -> isStudentWithinScope(sp, principal))
                .filter(sp -> requestedMinistryId == null || (sp.getMinistry() != null && sp.getMinistry().getId().equals(requestedMinistryId)))
                .filter(sp -> requestedClassId == null || (sp.getGradeClass() != null && sp.getGradeClass().getId().equals(requestedClassId)))
                .filter(sp -> requestedServantId == null || (sp.getResponsibleServant() != null && sp.getResponsibleServant().getId().equals(requestedServantId)))
                .toList();

        List<String> headers = List.of(
                "كود المخدوم",
                "الاسم الكامل",
                "رقم الهاتف",
                "النوع",
                "تاريخ الميلاد",
                "العنوان",
                "أب الاعتراف",
                "الخدمة",
                "الفصل",
                "الخادم المسؤول",
                "هاتف ولي الأمر",
                "المواهب",
                "ملاحظات إضافية",
                "الحالة"
        );

        List<List<?>> rows = new ArrayList<>();
        for (StudentPlacement sp : scoped) {
            Person p = sp.getPerson();
            rows.add(row(
                    p.getId(),
                    p.getFullName(),
                    p.getPhone(),
                    p.getGender() == Gender.MALE ? "ذكر" : "أنثى",
                    p.getDateOfBirth() != null ? p.getDateOfBirth().toString() : "",
                    p.getAddress() != null ? p.getAddress() : "",
                    p.getConfessionFather() != null ? p.getConfessionFather() : "",
                    sp.getMinistry() != null ? sp.getMinistry().getName() : "",
                    sp.getGradeClass() != null ? sp.getGradeClass().getName() : "",
                    sp.getResponsibleServant() != null ? sp.getResponsibleServant().getFullName() : "",
                    sp.getGuardianPhone() != null ? sp.getGuardianPhone() : "",
                    sp.getTalents() != null ? sp.getTalents() : "",
                    sp.getAdditionalDetails() != null ? sp.getAdditionalDetails() : "",
                    sp.getStatus() == StudentStatus.ACTIVE ? "نشط" : "متخرج"
            ));
        }

        return CsvUtil.generateCsvBytes(headers, rows);
    }

    /**
     * Exports weekly follow-up visits to CSV with role-scoped filtering.
     */
    @Transactional(readOnly = true)
    public byte[] exportVisitsCsv(VisitReportFilter filter, UserPrincipal principal) {
        Long requestedMinistryId = filter != null ? filter.ministryId() : null;
        Long requestedClassId = filter != null ? filter.classId() : null;
        Long requestedServantId = filter != null ? filter.servantId() : null;

        checkExplicitScope(requestedMinistryId, requestedClassId, requestedServantId, principal);

        Week week = resolveWeek(filter != null ? filter.weekId() : null);

        List<VisitRecord> visits = visitRecordRepository.findAllByWeekId(week.getId());

        List<VisitRecord> scoped = visits.stream()
                .filter(vr -> isVisitWithinScope(vr, principal))
                .filter(vr -> requestedMinistryId == null || (vr.getMinistrySnap() != null && vr.getMinistrySnap().getId().equals(requestedMinistryId)))
                .filter(vr -> requestedClassId == null || (vr.getClassSnap() != null && vr.getClassSnap().getId().equals(requestedClassId)))
                .filter(vr -> requestedServantId == null || (vr.getServantSnap() != null && vr.getServantSnap().getId().equals(requestedServantId)))
                .toList();

        List<String> headers = List.of(
                "كود الافتقاد",
                "بداية الأسبوع",
                "نهاية الأسبوع",
                "اسم المخدوم",
                "الخدمة",
                "الفصل",
                "الخادم المسؤول",
                "طريقة الافتقاد",
                "درجة الصلاة",
                "درجة القراءة",
                "درجة النوتة",
                "ملاحظات",
                "سجل بواسطة",
                "تاريخ التسجيل"
        );

        List<List<?>> rows = new ArrayList<>();
        for (VisitRecord vr : scoped) {
            rows.add(row(
                    vr.getId(),
                    vr.getWeek().getStartDate(),
                    vr.getWeek().getEndDate(),
                    vr.getStudent().getFullName(),
                    vr.getMinistrySnap() != null ? vr.getMinistrySnap().getName() : "",
                    vr.getClassSnap() != null ? vr.getClassSnap().getName() : "",
                    vr.getServantSnap() != null ? vr.getServantSnap().getFullName() : "",
                    vr.getMethod() == VisitMethod.VISIT ? "افتقاد (زيارة)" : "مكالمة",
                    vr.getPrayerScore() != null ? vr.getPrayerScore() : "",
                    vr.getReadingScore() != null ? vr.getReadingScore() : "",
                    vr.getNoteScore() != null ? vr.getNoteScore() : "",
                    vr.getNotes() != null ? vr.getNotes() : "",
                    (vr.getRecordedBy() != null && vr.getRecordedBy().getPerson() != null)
                            ? vr.getRecordedBy().getPerson().getFullName() : "",
                    vr.getRecordedAt()
            ));
        }

        return CsvUtil.generateCsvBytes(headers, rows);
    }

    /**
     * Exports attendance records for a week to CSV with role-scoped filtering.
     */
    @Transactional(readOnly = true)
    public byte[] exportAttendanceCsv(AttendanceReportFilter filter, UserPrincipal principal) {
        Long requestedMinistryId = filter != null ? filter.ministryId() : null;
        Long requestedClassId = filter != null ? filter.classId() : null;
        ActivityType requestedActivity = filter != null ? filter.activityType() : null;

        checkExplicitScope(requestedMinistryId, requestedClassId, null, principal);

        Week week = resolveWeek(filter != null ? filter.weekId() : null);
        AcademicYear currentYear = academicYearService.getCurrentEntity();

        List<AttendanceSession> sessions = attendanceSessionRepository.findAllByWeekId(week.getId());
        if (requestedActivity != null) {
            sessions = sessions.stream().filter(s -> s.getActivityType() == requestedActivity).toList();
        }

        // Cache student placements for the year for quick scope and class/ministry resolution
        List<StudentPlacement> yearPlacements = studentPlacementRepository.findAllByAcademicYearIdAndStatus(
                currentYear.getId(), StudentStatus.ACTIVE);
        Map<Long, StudentPlacement> placementByPersonId = new HashMap<>();
        for (StudentPlacement sp : yearPlacements) {
            placementByPersonId.put(sp.getPerson().getId(), sp);
        }

        List<String> headers = List.of(
                "كود الجلسة",
                "نوع النشاط",
                "تاريخ الجلسة",
                "كود المخدوم",
                "اسم المخدوم",
                "الخدمة",
                "الفصل",
                "حاضر",
                "سجل بواسطة",
                "تاريخ التسجيل"
        );

        List<List<?>> rows = new ArrayList<>();
        for (AttendanceSession session : sessions) {
            List<AttendanceRecord> records = attendanceRecordRepository.findAllBySessionId(session.getId());

            for (AttendanceRecord ar : records) {
                StudentPlacement sp = placementByPersonId.get(ar.getStudent().getId());
                if (sp != null && !isStudentWithinScope(sp, principal)) {
                    continue;
                }
                if (requestedMinistryId != null && (sp == null || sp.getMinistry() == null || !sp.getMinistry().getId().equals(requestedMinistryId))) {
                    continue;
                }
                if (requestedClassId != null && (sp == null || sp.getGradeClass() == null || !sp.getGradeClass().getId().equals(requestedClassId))) {
                    continue;
                }

                rows.add(row(
                        session.getId(),
                        formatActivityType(session.getActivityType()),
                        session.getSessionDate(),
                        ar.getStudent().getId(),
                        ar.getStudent().getFullName(),
                        sp != null && sp.getMinistry() != null ? sp.getMinistry().getName() : "",
                        sp != null && sp.getGradeClass() != null ? sp.getGradeClass().getName() : "",
                        ar.isPresent() ? "حاضر" : "غائب",
                        (ar.getRecordedBy() != null && ar.getRecordedBy().getPerson() != null)
                                ? ar.getRecordedBy().getPerson().getFullName() : "",
                        ar.getRecordedAt()
                ));
            }
        }

        return CsvUtil.generateCsvBytes(headers, rows);
    }

    /**
     * Exports confession records to CSV with role-scoped filtering.
     */
    @Transactional(readOnly = true)
    public byte[] exportConfessionsCsv(ConfessionReportFilter filter, UserPrincipal principal) {
        Long requestedMinistryId = filter != null ? filter.ministryId() : null;
        Long requestedClassId = filter != null ? filter.classId() : null;
        Long requestedStudentId = filter != null ? filter.studentId() : null;

        checkExplicitScope(requestedMinistryId, requestedClassId, null, principal);

        Long yearId = (filter != null && filter.academicYearId() != null)
                ? filter.academicYearId()
                : academicYearService.getCurrentEntity().getId();

        List<ConfessionRecord> confessions = confessionRecordRepository.findAllByAcademicYearId(yearId);

        // Preload student placements for scope check
        List<StudentPlacement> yearPlacements = studentPlacementRepository.findAllByAcademicYearIdAndStatus(
                yearId, StudentStatus.ACTIVE);
        Map<Long, StudentPlacement> placementByPersonId = new HashMap<>();
        for (StudentPlacement sp : yearPlacements) {
            placementByPersonId.put(sp.getPerson().getId(), sp);
        }

        List<String> headers = List.of(
                "كود الاعتراف",
                "العام الدراسي",
                "اسم المخدوم",
                "الخدمة",
                "الفصل",
                "تاريخ الاعتراف",
                "أب الاعتراف",
                "ملاحظات",
                "سجل بواسطة",
                "تاريخ التسجيل"
        );

        List<List<?>> rows = new ArrayList<>();
        for (ConfessionRecord cr : confessions) {
            if (requestedStudentId != null && !cr.getStudent().getId().equals(requestedStudentId)) {
                continue;
            }
            if (filter != null && filter.startDate() != null && cr.getConfessionDate().isBefore(filter.startDate())) {
                continue;
            }
            if (filter != null && filter.endDate() != null && cr.getConfessionDate().isAfter(filter.endDate())) {
                continue;
            }

            StudentPlacement sp = placementByPersonId.get(cr.getStudent().getId());
            if (sp != null && !isStudentWithinScope(sp, principal)) {
                continue;
            }
            if (requestedMinistryId != null && (sp == null || sp.getMinistry() == null || !sp.getMinistry().getId().equals(requestedMinistryId))) {
                continue;
            }
            if (requestedClassId != null && (sp == null || sp.getGradeClass() == null || !sp.getGradeClass().getId().equals(requestedClassId))) {
                continue;
            }

            rows.add(row(
                    cr.getId(),
                    cr.getAcademicYear().getName(),
                    cr.getStudent().getFullName(),
                    sp != null && sp.getMinistry() != null ? sp.getMinistry().getName() : "",
                    sp != null && sp.getGradeClass() != null ? sp.getGradeClass().getName() : "",
                    cr.getConfessionDate(),
                    cr.getConfessionFather() != null ? cr.getConfessionFather() : "",
                    cr.getNotes() != null ? cr.getNotes() : "",
                    (cr.getRecordedBy() != null && cr.getRecordedBy().getPerson() != null)
                            ? cr.getRecordedBy().getPerson().getFullName() : "",
                    cr.getCreatedAt()
            ));
        }

        return CsvUtil.generateCsvBytes(headers, rows);
    }

    // ── Internal Helpers ─────────────────────────────────────────────

    private Week resolveWeek(Long weekId) {
        if (weekId != null) {
            return weekService.getWeekOrThrow(weekId);
        }
        return weekService.getCurrentWeekEntity();
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

    private boolean isVisitWithinScope(VisitRecord vr, UserPrincipal principal) {
        if (principal == null) return false;
        if (principal.isAdmin()) return true;

        if (principal.isServiceSecretary()) {
            return vr.getMinistrySnap() != null && vr.getMinistrySnap().getId().equals(principal.getServiceSecretaryMinistryId());
        }
        if (principal.isClassSecretary()) {
            return vr.getClassSnap() != null && vr.getClassSnap().getId().equals(principal.getClassSecretaryClassId());
        }
        if (principal.isServant()) {
            return vr.getServantSnap() != null && vr.getServantSnap().getId().equals(principal.getPersonId());
        }
        return false;
    }

    private void checkExplicitScope(Long requestedMinistryId, Long requestedClassId, Long requestedServantId, UserPrincipal principal) {
        if (principal == null) {
            throw AppException.forbidden("ACCESS_DENIED", "غير مصرح");
        }
        if (principal.isAdmin()) {
            return;
        }

        if (principal.isServiceSecretary()) {
            if (requestedMinistryId != null && !requestedMinistryId.equals(principal.getServiceSecretaryMinistryId())) {
                throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك تصدير بيانات خدمة أخرى");
            }
            return;
        }

        if (principal.isClassSecretary()) {
            if (requestedClassId != null && !requestedClassId.equals(principal.getClassSecretaryClassId())) {
                throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك تصدير بيانات فصل آخر");
            }
            return;
        }

        if (principal.isServant()) {
            if (requestedServantId != null && !requestedServantId.equals(principal.getPersonId())) {
                throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك تصدير بيانات خادم آخر");
            }
            return;
        }

        throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية لتصدير التقارير");
    }

    private String formatActivityType(ActivityType activityType) {
        if (activityType == null) return "";
        return switch (activityType) {
            case MASS -> "قداس";
            case MEETING -> "اجتماع";
            case TASBEHA -> "تسبحة";
        };
    }

    private List<Object> row(Object... cells) {
        List<Object> list = new ArrayList<>(cells.length);
        for (Object cell : cells) {
            list.add(cell);
        }
        return list;
    }
}
