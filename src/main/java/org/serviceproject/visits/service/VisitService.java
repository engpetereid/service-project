package org.serviceproject.visits.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.service.AcademicYearService;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.settings.entity.SystemSetting;
import org.serviceproject.settings.repository.SystemSettingRepository;
import org.serviceproject.students.entity.StudentPlacement;
import org.serviceproject.students.entity.StudentStatus;
import org.serviceproject.students.repository.StudentPlacementRepository;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.repository.UserAccountRepository;
import org.serviceproject.visits.dto.ServantCurrentWeekResponse;
import org.serviceproject.visits.dto.ServantStudentVisitItem;
import org.serviceproject.visits.dto.UpdateVisitRequest;
import org.serviceproject.visits.dto.VisitRequest;
import org.serviceproject.visits.dto.VisitResponse;
import org.serviceproject.visits.dto.VisitSummaryDto;
import org.serviceproject.visits.entity.VisitRecord;
import org.serviceproject.visits.repository.VisitRecordRepository;
import org.serviceproject.weeks.dto.WeekResponse;
import org.serviceproject.weeks.entity.Week;
import org.serviceproject.weeks.service.WeekService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service managing weekly visits and follow-ups.
 * Enforces:
 * - Dynamic week locking (30-day threshold; admin override).
 * - Exact one visit record per (student, week).
 * - Scope-based authorization (Admin / Service Secretary / Class Secretary / Servant).
 * - Immutable placement snapshots (ministry, class, servant) at record time.
 * - Score range validation (Prayer 0-7, Reading 0-7, Note 0-MAX_NOTE_SCORE).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitRecordRepository visitRecordRepository;
    private final StudentPlacementRepository studentPlacementRepository;
    private final WeekService weekService;
    private final AcademicYearService academicYearService;
    private final UserAccountRepository userAccountRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional
    public ServantCurrentWeekResponse getCurrentWeekWorkflow(UserPrincipal principal) {
        WeekResponse weekResponse = weekService.getCurrentWeek();
        Week currentWeek = weekService.getCurrentWeekEntity();
        AcademicYear currentYear = academicYearService.getCurrentEntity();

        List<StudentPlacement> placements;

        if (principal.isAdmin()) {
            placements = studentPlacementRepository.findAllByAcademicYearIdAndStatus(
                    currentYear.getId(), StudentStatus.ACTIVE);
        } else if (principal.isServiceSecretary()) {
            placements = studentPlacementRepository.findAllByAcademicYearIdAndMinistryIdAndStatus(
                    currentYear.getId(), principal.getServiceSecretaryMinistryId(), StudentStatus.ACTIVE);
        } else if (principal.isClassSecretary()) {
            placements = studentPlacementRepository.findAllByAcademicYearIdAndClassIdAndStatus(
                    currentYear.getId(), principal.getClassSecretaryClassId(), StudentStatus.ACTIVE);
        } else if (principal.isServant()) {
            placements = studentPlacementRepository.findAllByAcademicYearIdAndServantIdAndStatus(
                    currentYear.getId(), principal.getPersonId(), StudentStatus.ACTIVE);
        } else {
            placements = List.of();
        }

        List<ServantStudentVisitItem> items = new ArrayList<>();
        for (StudentPlacement sp : placements) {
            Person student = sp.getPerson();
            Optional<VisitRecord> visitOpt = visitRecordRepository.findByStudentIdAndWeekId(student.getId(), currentWeek.getId());

            VisitSummaryDto visitSummary = visitOpt.map(v -> new VisitSummaryDto(
                    v.getId(),
                    v.getMethod().name(),
                    v.getPrayerScore(),
                    v.getReadingScore(),
                    v.getNoteScore(),
                    v.getNotes(),
                    v.getRecordedAt()
            )).orElse(null);

            Long classId = sp.getGradeClass() != null ? sp.getGradeClass().getId() : null;
            String className = sp.getGradeClass() != null ? sp.getGradeClass().getName() : null;
            Long servantId = sp.getResponsibleServant() != null ? sp.getResponsibleServant().getId() : null;
            String servantName = sp.getResponsibleServant() != null ? sp.getResponsibleServant().getFullName() : null;

            items.add(new ServantStudentVisitItem(
                    student.getId(),
                    student.getFullName(),
                    student.getPhone(),
                    student.getAddress(),
                    sp.getGuardianPhone(),
                    visitSummary,
                    classId,
                    className,
                    servantId,
                    servantName
            ));
        }

        return new ServantCurrentWeekResponse(weekResponse, items);
    }

    @Transactional(readOnly = true)
    public List<VisitResponse> findByWeek(Long weekId, UserPrincipal principal) {
        Week week = weekService.getWeekOrThrow(weekId);
        List<VisitRecord> records;

        if (principal.isAdmin()) {
            records = visitRecordRepository.findAllByWeekId(weekId);
        } else if (principal.isServiceSecretary()) {
            records = visitRecordRepository.findAllByWeekIdAndMinistrySnapId(weekId, principal.getServiceSecretaryMinistryId());
        } else if (principal.isClassSecretary()) {
            records = visitRecordRepository.findAllByWeekIdAndClassSnapId(weekId, principal.getClassSecretaryClassId());
        } else if (principal.isServant()) {
            records = visitRecordRepository.findAllByWeekIdAndServantSnapId(weekId, principal.getPersonId());
        } else {
            throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية لعرض سجلات الافتقاد");
        }

        return records.stream().map(r -> toResponse(r, principal)).toList();
    }

    @Transactional(readOnly = true)
    public VisitResponse findById(Long id, UserPrincipal principal) {
        VisitRecord record = visitRecordRepository.findByIdWithDetails(id)
                .orElseThrow(() -> AppException.notFound("VISIT_NOT_FOUND", "سجل الافتقاد غير موجود"));

        checkReadScope(record, principal);
        return toResponse(record, principal);
    }

    @Transactional
    public VisitResponse create(VisitRequest request, UserPrincipal principal) {
        Week week = weekService.getWeekOrThrow(request.weekId());

        // 1. Dynamic week lock enforcement
        if (weekService.isWeekLockedForUser(week, principal)) {
            throw AppException.forbidden("WEEK_LOCKED", "لا يمكن تسجيل افتقاد في أسبوع مقفول (مضى عليه أكثر من 30 يوماً)");
        }

        // 2. Uniqueness check per (student, week)
        if (visitRecordRepository.existsByStudentIdAndWeekId(request.studentId(), request.weekId())) {
            throw AppException.conflict("VISIT_EXISTS", "تم تسجيل افتقاد لهذا المخدوم في هذا الأسبوع بالفعل");
        }

        // 3. Find active student placement
        AcademicYear currentYear = academicYearService.getCurrentEntity();
        StudentPlacement placement = studentPlacementRepository.findByPersonIdAndAcademicYearId(request.studentId(), currentYear.getId())
                .orElseThrow(() -> AppException.notFound("STUDENT_NOT_PLACED", "المخدوم غير مسكن في العام الدراسي الحالي"));

        if (placement.getStatus() != StudentStatus.ACTIVE) {
            throw AppException.badRequest("STUDENT_NOT_ACTIVE", "لا يمكن تسجيل افتقاد لمخدوم غير نشط أو متخرج");
        }

        // 4. Scope authorization check
        checkRecordScope(placement, principal);

        // 5. Score validations
        validateScores(request.prayerScore(), request.readingScore(), request.noteScore());

        // 6. Recorder identity
        UserAccount recorder = userAccountRepository.findById(principal.getUserId())
                .orElseThrow(() -> AppException.unauthorized("RECORDER_NOT_FOUND", "المستخدم المسجل غير موجود"));

        // 7. Save record with immutable placement snapshots
        int maxNoteScore = getMaxNoteScore();
        VisitRecord visitRecord = new VisitRecord(
                placement.getPerson(),
                week,
                currentYear,
                placement.getMinistry(),
                placement.getGradeClass(),
                placement.getResponsibleServant(),
                request.method(),
                request.prayerScore(),
                request.readingScore(),
                request.noteScore(),
                maxNoteScore,
                request.notes(),
                recorder
        );

        visitRecord = visitRecordRepository.save(visitRecord);
        return toResponse(visitRecord, principal);
    }

    @Transactional
    public VisitResponse update(Long id, UpdateVisitRequest request, UserPrincipal principal) {
        VisitRecord record = visitRecordRepository.findByIdWithDetails(id)
                .orElseThrow(() -> AppException.notFound("VISIT_NOT_FOUND", "سجل الافتقاد غير موجود"));

        // Dynamic week lock enforcement
        if (weekService.isWeekLockedForUser(record.getWeek(), principal)) {
            throw AppException.forbidden("WEEK_LOCKED", "لا يمكن تعديل افتقاد في أسبوع مقفول (مضى عليه أكثر من 30 يوماً)");
        }

        // Scope check
        checkEditScope(record, principal);

        // Score validations
        validateScores(request.prayerScore(), request.readingScore(), request.noteScore());

        record.setMethod(request.method());
        record.setPrayerScore(request.prayerScore());
        record.setReadingScore(request.readingScore());
        record.setNoteScore(request.noteScore());
        record.setNotes(request.notes());

        record = visitRecordRepository.save(record);

        if (principal != null && principal.isAdmin() && eventPublisher != null) {
            boolean isLockedForRegular = org.serviceproject.common.util.DateUtil.isWeekLocked(
                    record.getWeek().getEndDate(), 30, "Africa/Cairo");
            if (isLockedForRegular) {
                eventPublisher.publishEvent(org.serviceproject.audit.event.AuditEvent.of(
                        principal,
                        org.serviceproject.audit.entity.AuditAction.LOCK_OVERRIDE,
                        "VisitRecord",
                        record.getId(),
                        "Locked week " + record.getWeek().getId(),
                        "Admin override update"
                ));
            }
        }

        return toResponse(record, principal);
    }

    // ── Internal Scope & Validation Helpers ──────────────────────────

    private void checkReadScope(VisitRecord record, UserPrincipal principal) {
        if (principal.isAdmin()) return;

        if (principal.isServiceSecretary()) {
            if (record.getMinistrySnap().getId().equals(principal.getServiceSecretaryMinistryId())) return;
        }

        if (principal.isClassSecretary()) {
            if (record.getClassSnap().getId().equals(principal.getClassSecretaryClassId())) return;
        }

        if (principal.isServant()) {
            if (record.getServantSnap() != null && record.getServantSnap().getId().equals(principal.getPersonId())) return;
        }

        throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية لعرض هذا الافتقاد");
    }

    private void checkRecordScope(StudentPlacement placement, UserPrincipal principal) {
        if (principal.isAdmin()) return;

        if (principal.isServiceSecretary()) {
            if (placement.getMinistry() != null && placement.getMinistry().getId().equals(principal.getServiceSecretaryMinistryId())) return;
            throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك تسجيل افتقاد لمخدوم خارج خدمتك المصرح بها");
        }

        if (principal.isClassSecretary()) {
            if (placement.getGradeClass() != null && placement.getGradeClass().getId().equals(principal.getClassSecretaryClassId())) return;
            throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك تسجيل افتقاد لمخدوم خارج فصلك المصرح به");
        }

        if (principal.isServant()) {
            if (placement.getResponsibleServant() != null && placement.getResponsibleServant().getId().equals(principal.getPersonId())) return;
            throw AppException.forbidden("ACCESS_DENIED", "يمكنك فقط تسجيل افتقاد للمخدومين المسندين إليك");
        }

        throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية لتسجيل الافتقاد");
    }

    private void checkEditScope(VisitRecord record, UserPrincipal principal) {
        if (principal.isAdmin()) return;

        if (principal.isServiceSecretary()) {
            if (record.getMinistrySnap().getId().equals(principal.getServiceSecretaryMinistryId())) return;
            throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك تعديل افتقاد خارج خدمتك المصرح بها");
        }

        if (principal.isClassSecretary()) {
            if (record.getClassSnap().getId().equals(principal.getClassSecretaryClassId())) return;
            throw AppException.forbidden("ACCESS_DENIED", "لا يمكنك تعديل افتقاد خارج فصلك المصرح به");
        }

        if (principal.isServant()) {
            if (record.getServantSnap() != null && record.getServantSnap().getId().equals(principal.getPersonId())) return;
            throw AppException.forbidden("ACCESS_DENIED", "يمكنك فقط تعديل الافتقاد للمخدومين المسندين إليك");
        }

        throw AppException.forbidden("ACCESS_DENIED", "ليس لديك صلاحية لتعديل الافتقاد");
    }

    private void validateScores(Integer prayerScore, Integer readingScore, Integer noteScore) {
        if (prayerScore != null && (prayerScore < 0 || prayerScore > 7)) {
            throw AppException.badRequest("INVALID_PRAYER_SCORE", "درجة الصلاة يجب أن تكون بين 0 و 7");
        }
        if (readingScore != null && (readingScore < 0 || readingScore > 7)) {
            throw AppException.badRequest("INVALID_READING_SCORE", "درجة القراءة يجب أن تكون بين 0 و 7");
        }
        if (noteScore != null) {
            int maxNoteScore = getMaxNoteScore();
            if (noteScore < 0 || noteScore > maxNoteScore) {
                throw AppException.badRequest("INVALID_NOTE_SCORE", "درجة النوتة يجب أن تكون بين 0 و " + maxNoteScore);
            }
        }
    }

    private int getMaxNoteScore() {
        return systemSettingRepository.findBySettingKey("MAX_NOTE_SCORE")
                .map(SystemSetting::getSettingValue)
                .map(val -> {
                    try {
                        return Integer.parseInt(val.trim());
                    } catch (NumberFormatException e) {
                        return 21;
                    }
                })
                .orElse(21);
    }

    private VisitResponse toResponse(VisitRecord vr, UserPrincipal principal) {
        boolean locked = weekService.isWeekLockedForUser(vr.getWeek(), principal);

        return new VisitResponse(
                vr.getId(),
                vr.getStudent().getId(),
                vr.getStudent().getFullName(),
                vr.getWeek().getId(),
                vr.getWeek().getStartDate(),
                vr.getWeek().getEndDate(),
                locked,
                vr.getAcademicYear().getId(),
                vr.getAcademicYear().getName(),
                vr.getMinistrySnap().getId(),
                vr.getMinistrySnap().getName(),
                vr.getClassSnap().getId(),
                vr.getClassSnap().getName(),
                vr.getServantSnap() != null ? vr.getServantSnap().getId() : null,
                vr.getServantSnap() != null ? vr.getServantSnap().getFullName() : null,
                vr.getMethod().name(),
                vr.getPrayerScore(),
                vr.getReadingScore(),
                vr.getNoteScore(),
                vr.getMaxNoteScoreSnapshot(),
                vr.getNotes(),
                vr.getRecordedBy().getId(),
                vr.getRecordedBy().getPerson().getFullName(),
                vr.getRecordedAt()
        );
    }
}
