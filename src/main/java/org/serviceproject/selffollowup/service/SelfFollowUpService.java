package org.serviceproject.selffollowup.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.service.AcademicYearService;
import org.serviceproject.audit.entity.AuditAction;
import org.serviceproject.audit.event.AuditEvent;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.selffollowup.dto.SelfFollowUpCurrentWeekResponse;
import org.serviceproject.selffollowup.dto.SelfFollowUpRequest;
import org.serviceproject.selffollowup.dto.SelfFollowUpResponse;
import org.serviceproject.selffollowup.dto.SelfFollowUpStatsResponse;
import org.serviceproject.selffollowup.entity.ServantWeeklyFollowUp;
import org.serviceproject.selffollowup.repository.ServantWeeklyFollowUpRepository;
import org.serviceproject.settings.service.SettingsService;
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.users.repository.UserAccountRepository;
import org.serviceproject.weeks.dto.WeekResponse;
import org.serviceproject.weeks.entity.Week;
import org.serviceproject.weeks.repository.WeekRepository;
import org.serviceproject.weeks.service.WeekService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Service managing personal weekly self-follow-up for servants and class secretaries.
 * <p>
 * Enforces:
 * <ul>
 *   <li>Only SERVANT, CLASS_SECRETARY (and GENERAL_ADMIN) can access.</li>
 *   <li>Each servant records only their own follow-up (tied to {@code principal.getUserId()}).</li>
 *   <li>Exactly one record per (user, week).</li>
 *   <li>Dynamic 30-day week locking via {@link WeekService#isWeekLockedForUser}.</li>
 *   <li>Snapshot of {@code maxNoteScore} frozen at creation time to protect historical accuracy.</li>
 *   <li>5 equal-weight metrics for overall percentage; omitted (null) metrics excluded from average.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SelfFollowUpService {

    private final ServantWeeklyFollowUpRepository servantWeeklyFollowUpRepository;
    private final WeekService weekService;
    private final WeekRepository weekRepository;
    private final AcademicYearService academicYearService;
    private final UserAccountRepository userAccountRepository;
    private final SettingsService settingsService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SelfFollowUpCurrentWeekResponse getCurrentWeek(UserPrincipal principal) {
        checkAccess(principal);

        WeekResponse weekResponse = weekService.getCurrentWeek();
        Week currentWeek = weekService.getCurrentWeekEntity();
        int currentMaxNoteScore = settingsService.getMaxNoteScore();

        SelfFollowUpResponse recordResponse = servantWeeklyFollowUpRepository
                .findByUserIdAndWeekId(principal.getUserId(), currentWeek.getId())
                .map(r -> toResponse(r, principal))
                .orElse(null);

        return new SelfFollowUpCurrentWeekResponse(weekResponse, recordResponse, currentMaxNoteScore);
    }

    @Transactional
    public SelfFollowUpResponse upsert(SelfFollowUpRequest request, UserPrincipal principal) {
        checkAccess(principal);

        Week week = weekService.getWeekOrThrow(request.weekId());

        // 1. Dynamic week lock enforcement
        if (weekService.isWeekLockedForUser(week, principal)) {
            throw AppException.forbidden("WEEK_LOCKED", "لا يمكن تسجيل أو تعديل المتابعة في أسبوع مقفول (مضى عليه أكثر من 30 يوماً)");
        }

        Long userId = principal.getUserId();
        Optional<ServantWeeklyFollowUp> existingOpt = servantWeeklyFollowUpRepository.findByUserIdAndWeekId(userId, week.getId());

        ServantWeeklyFollowUp record;
        boolean isNew = existingOpt.isEmpty();

        if (isNew) {
            int currentMax = settingsService.getMaxNoteScore();
            validateScore(request.noteScore(), currentMax);

            AcademicYear currentYear = academicYearService.getCurrentEntity();
            UserAccount user = userAccountRepository.findById(userId)
                    .orElseThrow(() -> AppException.unauthorized("USER_NOT_FOUND", "المستخدم غير موجود"));

            record = new ServantWeeklyFollowUp(
                    user,
                    week,
                    currentYear,
                    request.noteScore(),
                    currentMax,
                    request.attendedMass(),
                    request.attendedServiceMeeting(),
                    request.attendedTasbeha(),
                    request.attendedManagementMeeting()
            );
            record = servantWeeklyFollowUpRepository.save(record);

            if (eventPublisher != null) {
                eventPublisher.publishEvent(AuditEvent.of(
                        principal,
                        AuditAction.CREATE,
                        "ServantWeeklyFollowUp",
                        record.getId(),
                        null,
                        "Created weekly self-follow-up for week " + week.getId()
                ));
            }
        } else {
            record = existingOpt.get();
            int maxSnapshot = record.getMaxNoteScoreSnapshot();
            validateScore(request.noteScore(), maxSnapshot);

            record.setNoteScore(request.noteScore());
            record.setAttendedMass(request.attendedMass());
            record.setAttendedServiceMeeting(request.attendedServiceMeeting());
            record.setAttendedTasbeha(request.attendedTasbeha());
            record.setAttendedManagementMeeting(request.attendedManagementMeeting());

            record = servantWeeklyFollowUpRepository.save(record);

            if (eventPublisher != null) {
                eventPublisher.publishEvent(AuditEvent.of(
                        principal,
                        AuditAction.UPDATE,
                        "ServantWeeklyFollowUp",
                        record.getId(),
                        null,
                        "Updated weekly self-follow-up for week " + week.getId()
                ));
            }
        }

        return toResponse(record, principal);
    }

    @Transactional(readOnly = true)
    public SelfFollowUpResponse getByWeek(Long weekId, UserPrincipal principal) {
        checkAccess(principal);
        Week week = weekService.getWeekOrThrow(weekId);

        ServantWeeklyFollowUp record = servantWeeklyFollowUpRepository
                .findByUserIdAndWeekId(principal.getUserId(), week.getId())
                .orElseThrow(() -> AppException.notFound("FOLLOWUP_NOT_FOUND", "لم يتم تسجيل متابعة لهذا الأسبوع بعد"));

        return toResponse(record, principal);
    }

    @Transactional(readOnly = true)
    public List<SelfFollowUpResponse> getHistory(UserPrincipal principal) {
        checkAccess(principal);
        AcademicYear currentYear = academicYearService.getCurrentEntity();

        List<ServantWeeklyFollowUp> records = servantWeeklyFollowUpRepository
                .findAllByUserIdAndAcademicYearIdOrderByWeekStartDateDesc(principal.getUserId(), currentYear.getId());

        return records.stream().map(r -> toResponse(r, principal)).toList();
    }

    @Transactional(readOnly = true)
    public SelfFollowUpStatsResponse getStatistics(UserPrincipal principal) {
        checkAccess(principal);
        AcademicYear currentYear = academicYearService.getCurrentEntity();

        List<Week> allWeeks = weekRepository.findAllByDeletedAtIsNullOrderByStartDateDesc();
        List<Week> currentYearWeeks = (currentYear != null && currentYear.getStartDate() != null && currentYear.getEndDate() != null)
                ? allWeeks.stream()
                        .filter(w -> !w.getStartDate().isBefore(currentYear.getStartDate()) && !w.getEndDate().isAfter(currentYear.getEndDate()))
                        .toList()
                : Collections.emptyList();
        int totalWeeks = currentYearWeeks.isEmpty() ? allWeeks.size() : currentYearWeeks.size();

        List<ServantWeeklyFollowUp> recordsAsc = servantWeeklyFollowUpRepository
                .findAllByUserIdAndAcademicYearIdOrderByWeekStartDateAsc(principal.getUserId(), currentYear.getId());

        long recordedWeeks = recordsAsc.size();
        double recordingRate = totalWeeks > 0 ? roundOneDecimal((recordedWeeks * 100.0) / totalWeeks) : 0.0;

        Double avgNotePercentage = recordsAsc.stream()
                .filter(r -> r.getNoteScore() != null)
                .mapToDouble(r -> {
                    int max = r.getMaxNoteScoreSnapshot() > 0 ? r.getMaxNoteScoreSnapshot() : 21;
                    return Math.min(100.0, (r.getNoteScore() * 100.0) / max);
                })
                .average()
                .stream()
                .map(this::roundOneDecimal)
                .boxed()
                .findFirst()
                .orElse(null);

        Double avgMassRate = calculateBooleanRate(recordsAsc, ServantWeeklyFollowUp::getAttendedMass);
        Double avgServiceMeetingRate = calculateBooleanRate(recordsAsc, ServantWeeklyFollowUp::getAttendedServiceMeeting);
        Double avgTasbehaRate = calculateBooleanRate(recordsAsc, ServantWeeklyFollowUp::getAttendedTasbeha);
        Double avgManagementMeetingRate = calculateBooleanRate(recordsAsc, ServantWeeklyFollowUp::getAttendedManagementMeeting);

        Double avgOverallPercentage = recordsAsc.stream()
                .map(this::computeOverallPercentage)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .stream()
                .map(this::roundOneDecimal)
                .boxed()
                .findFirst()
                .orElse(null);

        List<SelfFollowUpStatsResponse.WeeklyTrendPoint> weeklyTrend = recordsAsc.stream()
                .map(r -> new SelfFollowUpStatsResponse.WeeklyTrendPoint(
                        r.getWeek().getId(),
                        r.getWeek().getStartDate(),
                        r.getWeek().getEndDate(),
                        computeOverallPercentage(r)
                ))
                .toList();

        return new SelfFollowUpStatsResponse(
                totalWeeks,
                recordedWeeks,
                recordingRate,
                avgNotePercentage,
                avgMassRate,
                avgServiceMeetingRate,
                avgTasbehaRate,
                avgManagementMeetingRate,
                avgOverallPercentage,
                weeklyTrend
        );
    }

    // ── Internal Helpers ─────────────────────────────────────────────

    private void checkAccess(UserPrincipal principal) {
        if (principal == null) {
            throw AppException.unauthorized("UNAUTHORIZED", "يجب تسجيل الدخول أولاً");
        }
        if (!principal.isServant() && !principal.isClassSecretary() && !principal.isAdmin()) {
            throw AppException.forbidden("ACCESS_DENIED", "متابعتي الأسبوعية مخصصة للخدام وأمناء الفصول فقط");
        }
    }

    private void validateScore(Integer noteScore, int maxScore) {
        if (noteScore != null) {
            if (noteScore < 0) {
                throw AppException.badRequest("INVALID_NOTE_SCORE", "درجة النوتة لا يمكن أن تكون سالبة");
            }
            if (noteScore > maxScore) {
                throw AppException.badRequest("INVALID_NOTE_SCORE", "درجة النوتة يجب أن تكون بين 0 و " + maxScore);
            }
        }
    }

    public static Double calculateOverallPercentage(
            Integer noteScore, Integer maxNoteScoreSnapshot,
            Boolean attendedMass, Boolean attendedServiceMeeting,
            Boolean attendedTasbeha, Boolean attendedManagementMeeting) {

        List<Double> filledMetrics = new ArrayList<>();

        if (attendedMass != null) {
            filledMetrics.add(attendedMass ? 100.0 : 0.0);
        }
        if (attendedServiceMeeting != null) {
            filledMetrics.add(attendedServiceMeeting ? 100.0 : 0.0);
        }
        if (attendedTasbeha != null) {
            filledMetrics.add(attendedTasbeha ? 100.0 : 0.0);
        }
        if (attendedManagementMeeting != null) {
            filledMetrics.add(attendedManagementMeeting ? 100.0 : 0.0);
        }
        if (noteScore != null) {
            int max = (maxNoteScoreSnapshot != null && maxNoteScoreSnapshot > 0) ? maxNoteScoreSnapshot : 21;
            double notePercent = Math.min(100.0, (noteScore * 100.0) / max);
            filledMetrics.add(notePercent);
        }

        if (filledMetrics.isEmpty()) {
            return 0.0;
        }

        double avg = filledMetrics.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return Math.round(avg * 10.0) / 10.0;
    }

    private Double computeOverallPercentage(ServantWeeklyFollowUp record) {
        return calculateOverallPercentage(
                record.getNoteScore(),
                record.getMaxNoteScoreSnapshot(),
                record.getAttendedMass(),
                record.getAttendedServiceMeeting(),
                record.getAttendedTasbeha(),
                record.getAttendedManagementMeeting()
        );
    }

    private Double calculateBooleanRate(List<ServantWeeklyFollowUp> records, Function<ServantWeeklyFollowUp, Boolean> extractor) {
        long filledCount = records.stream().map(extractor).filter(Objects::nonNull).count();
        if (filledCount == 0) return null;
        long trueCount = records.stream().map(extractor).filter(Boolean.TRUE::equals).count();
        return roundOneDecimal((trueCount * 100.0) / filledCount);
    }

    private double roundOneDecimal(double val) {
        return Math.round(val * 10.0) / 10.0;
    }

    private SelfFollowUpResponse toResponse(ServantWeeklyFollowUp r, UserPrincipal principal) {
        boolean locked = weekService.isWeekLockedForUser(r.getWeek(), principal);

        return new SelfFollowUpResponse(
                r.getId(),
                r.getWeek().getId(),
                r.getWeek().getStartDate(),
                r.getWeek().getEndDate(),
                locked,
                r.getAcademicYear().getId(),
                r.getAcademicYear().getName(),
                r.getNoteScore(),
                r.getMaxNoteScoreSnapshot(),
                r.getAttendedMass(),
                r.getAttendedServiceMeeting(),
                r.getAttendedTasbeha(),
                r.getAttendedManagementMeeting(),
                computeOverallPercentage(r),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
