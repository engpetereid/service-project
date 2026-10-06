package org.serviceproject.attendance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.service.AcademicYearService;
import org.serviceproject.attendance.dto.AttendanceRecordResponse;
import org.serviceproject.attendance.dto.AttendanceSessionResponse;
import org.serviceproject.attendance.dto.BatchToggleAttendanceRequest;
import org.serviceproject.attendance.dto.CreateSessionRequest;
import org.serviceproject.attendance.dto.SessionDetailResponse;
import org.serviceproject.attendance.dto.ToggleAttendanceRequest;
import org.serviceproject.attendance.entity.AttendanceRecord;
import org.serviceproject.attendance.entity.AttendanceSession;
import org.serviceproject.attendance.repository.AttendanceRecordRepository;
import org.serviceproject.attendance.repository.AttendanceSessionRepository;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.students.entity.StudentPlacement;
import org.serviceproject.students.entity.StudentStatus;
import org.serviceproject.students.repository.StudentPlacementRepository;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.repository.PersonRepository;
import org.serviceproject.users.repository.UserAccountRepository;
import org.serviceproject.weeks.entity.Week;
import org.serviceproject.weeks.service.WeekService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service managing attendance sessions and student attendance records.
 * <p>
 * Key Business Rules:
 * 1. Scope Exception: ANY authenticated user with a recognized role can record attendance for ALL active students.
 * 2. Dynamic 30-Day Lock: Attendance follows the week's lock threshold (admin override applies).
 * 3. Exactly one record per (session, student).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final StudentPlacementRepository studentPlacementRepository;
    private final PersonRepository personRepository;
    private final WeekService weekService;
    private final AcademicYearService academicYearService;
    private final UserAccountRepository userAccountRepository;

    @Transactional
    public AttendanceSessionResponse createSession(CreateSessionRequest request, UserPrincipal principal) {
        Week week = weekService.getWeekOrThrow(request.weekId());
        Week currentWeek = weekService.getCurrentWeekEntity();

        if (principal != null && !principal.isAdmin() && currentWeek != null && !week.getId().equals(currentWeek.getId())) {
            throw AppException.badRequest("CURRENT_WEEK_ONLY", "يمكن فتح وإنشاء الاجتماعات في الأسبوع الحالي فقط");
        }

        if (weekService.isWeekLockedForUser(week, principal)) {
            throw AppException.forbidden("WEEK_LOCKED", "لا يمكن إنشاء جلسة حضور في أسبوع مقفول");
        }

        if (attendanceSessionRepository.existsByWeekIdAndActivityTypeAndSessionDate(
                request.weekId(), request.activityType(), request.sessionDate())) {
            throw AppException.conflict("SESSION_EXISTS", "توجد جلسة حضور مسجلة بالفعل لنفس النشاط والتاريخ في هذا الأسبوع");
        }

        AttendanceSession session = new AttendanceSession(week, request.activityType(), request.sessionDate());
        session = attendanceSessionRepository.save(session);

        return toSessionResponse(session);
    }

    @Transactional(readOnly = true)
    public List<AttendanceSessionResponse> getSessionsByWeek(Long weekId) {
        weekService.getWeekOrThrow(weekId);
        return attendanceSessionRepository.findAllByWeekId(weekId).stream()
                .map(this::toSessionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionDetailResponse getSessionDetails(Long sessionId) {
        AttendanceSession session = attendanceSessionRepository.findByIdWithWeek(sessionId)
                .orElseThrow(() -> AppException.notFound("SESSION_NOT_FOUND", "جلسة الحضور غير موجودة"));

        List<AttendanceRecordResponse> records = attendanceRecordRepository.findAllBySessionId(sessionId).stream()
                .map(this::toRecordResponse)
                .toList();

        return new SessionDetailResponse(toSessionResponse(session), records);
    }

    @Transactional
    public AttendanceRecordResponse toggleAttendance(ToggleAttendanceRequest request, UserPrincipal principal) {
        AttendanceSession session = attendanceSessionRepository.findByIdWithWeek(request.sessionId())
                .orElseThrow(() -> AppException.notFound("SESSION_NOT_FOUND", "جلسة الحضور غير موجودة"));

        // Dynamic week lock check
        if (weekService.isWeekLockedForUser(session.getWeek(), principal)) {
            throw AppException.forbidden("WEEK_LOCKED", "لا يمكن تعديل الحضور في أسبوع مقفول");
        }

        Person student = personRepository.findByIdAndDeletedAtIsNull(request.studentId())
                .orElseThrow(() -> AppException.notFound("STUDENT_NOT_FOUND", "المخدوم غير موجود"));

        // Verify student is active in current academic year
        AcademicYear currentYear = academicYearService.getCurrentEntity();
        StudentPlacement placement = studentPlacementRepository.findByPersonIdAndAcademicYearId(student.getId(), currentYear.getId())
                .orElseThrow(() -> AppException.notFound("STUDENT_NOT_PLACED", "المخدوم غير مسكن في العام الدراسي الحالي"));

        if (placement.getStatus() != StudentStatus.ACTIVE) {
            throw AppException.badRequest("STUDENT_NOT_ACTIVE", "لا يمكن تسجيل حضور لمخدوم غير نشط أو متخرج");
        }

        // ATTENDANCE EXCEPTION: Scope check is intentionally bypassed.
        // Any authenticated user can record attendance for any active student.

        UserAccount recorder = userAccountRepository.findById(principal.getUserId())
                .orElseThrow(() -> AppException.unauthorized("RECORDER_NOT_FOUND", "المستخدم المسجل غير موجود"));

        Optional<AttendanceRecord> existingOpt = attendanceRecordRepository.findBySessionIdAndStudentId(
                request.sessionId(), request.studentId());

        AttendanceRecord record;
        if (existingOpt.isPresent()) {
            record = existingOpt.get();
            record.setPresent(request.present());
            record.setRecordedBy(recorder);
            record.setRecordedAt(LocalDateTime.now());
        } else {
            record = new AttendanceRecord(session, student, request.present(), recorder);
        }

        record = attendanceRecordRepository.save(record);
        return toRecordResponse(record);
    }

    @Transactional
    public List<AttendanceRecordResponse> batchToggleAttendance(BatchToggleAttendanceRequest request, UserPrincipal principal) {
        AttendanceSession session = attendanceSessionRepository.findByIdWithWeek(request.sessionId())
                .orElseThrow(() -> AppException.notFound("SESSION_NOT_FOUND", "جلسة الحضور غير موجودة"));

        if (weekService.isWeekLockedForUser(session.getWeek(), principal)) {
            throw AppException.forbidden("WEEK_LOCKED", "لا يمكن تعديل الحضور في أسبوع مقفول");
        }

        AcademicYear currentYear = academicYearService.getCurrentEntity();
        UserAccount recorder = userAccountRepository.findById(principal.getUserId())
                .orElseThrow(() -> AppException.unauthorized("RECORDER_NOT_FOUND", "المستخدم المسجل غير موجود"));

        List<AttendanceRecord> updated = new java.util.ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Long studentId : request.studentIds()) {
            Person student = personRepository.findByIdAndDeletedAtIsNull(studentId)
                    .orElseThrow(() -> AppException.notFound("STUDENT_NOT_FOUND", "المخدوم غير موجود: " + studentId));

            StudentPlacement placement = studentPlacementRepository.findByPersonIdAndAcademicYearId(student.getId(), currentYear.getId())
                    .orElseThrow(() -> AppException.notFound("STUDENT_NOT_PLACED", "المخدوم غير مسكن في العام الدراسي الحالي: " + student.getFullName()));

            if (placement.getStatus() != StudentStatus.ACTIVE) {
                continue;
            }

            Optional<AttendanceRecord> existingOpt = attendanceRecordRepository.findBySessionIdAndStudentId(
                    request.sessionId(), student.getId());

            AttendanceRecord record;
            if (existingOpt.isPresent()) {
                record = existingOpt.get();
                record.setPresent(request.present());
                record.setRecordedBy(recorder);
                record.setRecordedAt(now);
            } else {
                record = new AttendanceRecord(session, student, request.present(), recorder);
            }
            updated.add(attendanceRecordRepository.save(record));
        }

        return updated.stream().map(this::toRecordResponse).toList();
    }

    // ── Internal Helpers ─────────────────────────────────────────────

    private AttendanceSessionResponse toSessionResponse(AttendanceSession session) {
        long presentCount = attendanceRecordRepository.countPresentBySessionId(session.getId());
        Week w = session.getWeek();

        return new AttendanceSessionResponse(
                session.getId(),
                w.getId(),
                w.getStartDate(),
                w.getEndDate(),
                session.getActivityType().name(),
                session.getSessionDate(),
                presentCount
        );
    }

    private AttendanceRecordResponse toRecordResponse(AttendanceRecord record) {
        return new AttendanceRecordResponse(
                record.getId(),
                record.getSession().getId(),
                record.getStudent().getId(),
                record.getStudent().getFullName(),
                record.isPresent(),
                record.getRecordedBy().getId(),
                record.getRecordedBy().getPerson().getFullName(),
                record.getRecordedAt()
        );
    }
}
