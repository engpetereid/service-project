package org.serviceproject.weeks.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serviceproject.common.config.AppProperties;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.security.UserPrincipal;
import org.serviceproject.common.util.DateUtil;
import org.serviceproject.weeks.dto.WeekRequest;
import org.serviceproject.weeks.dto.WeekResponse;
import org.serviceproject.weeks.entity.Week;
import org.serviceproject.weeks.repository.WeekRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service managing weeks and dynamic locking.
 * <p>
 * A week is dynamically locked when {@code endDate + 30 days < today}.
 * Lock state is never stored as a column.
 * Only GENERAL_ADMIN can override locks, edit weeks, or soft-delete/restore them.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeekService {

    private final WeekRepository weekRepository;
    private final AppProperties appProperties;

    @Transactional
    public WeekResponse getCurrentWeek() {
        Week week = getCurrentWeekEntity();
        return toResponse(week);
    }

    @Transactional
    public Week getCurrentWeekEntity() {
        LocalDate today = DateUtil.today(appProperties.timeZone());
        LocalDate startDate = DateUtil.weekStartDate(today);

        return ensureWeek(startDate);
    }

    /**
     * Ensures a week exists for the given Friday start date.
     * Idempotent.
     */
    @Transactional
    public Week ensureWeek(LocalDate startDate) {
        Optional<Week> existingOpt = weekRepository.findByStartDate(startDate);
        if (existingOpt.isPresent()) {
            Week existing = existingOpt.get();
            if (existing.isDeleted()) {
                existing.restore();
                return weekRepository.save(existing);
            }
            return existing;
        }

        LocalDate endDate = startDate.plusDays(6);
        Week week = new Week(startDate, endDate);
        week = weekRepository.save(week);
        log.info("Created week: {} to {}", startDate, endDate);
        return week;
    }

    @Transactional(readOnly = true)
    public List<WeekResponse> findAll(boolean includeDeleted) {
        List<Week> weeks = includeDeleted
                ? weekRepository.findAllByOrderByStartDateDesc()
                : weekRepository.findAllByDeletedAtIsNullOrderByStartDateDesc();

        return weeks.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public WeekResponse findById(Long id) {
        Week week = getWeekOrThrow(id);
        return toResponse(week);
    }

    @Transactional
    public WeekResponse create(WeekRequest request) {
        if (!request.startDate().isBefore(request.endDate())) {
            throw AppException.badRequest("INVALID_DATES", "تاريخ البداية يجب أن يكون قبل تاريخ النهاية");
        }

        if (weekRepository.existsByStartDate(request.startDate())) {
            throw AppException.conflict("WEEK_EXISTS", "يوجد أسبوع يبدأ بالفعل في هذا التاريخ");
        }

        Week week = new Week(request.startDate(), request.endDate());
        week = weekRepository.save(week);
        return toResponse(week);
    }

    @Transactional
    public WeekResponse update(Long id, WeekRequest request) {
        Week week = getWeekOrThrow(id);

        if (!request.startDate().isBefore(request.endDate())) {
            throw AppException.badRequest("INVALID_DATES", "تاريخ البداية يجب أن يكون قبل تاريخ النهاية");
        }

        if (!week.getStartDate().equals(request.startDate())
                && weekRepository.existsByStartDate(request.startDate())) {
            throw AppException.conflict("WEEK_EXISTS", "يوجد أسبوع يبدأ بالفعل في هذا التاريخ");
        }

        week.setStartDate(request.startDate());
        week.setEndDate(request.endDate());
        week = weekRepository.save(week);
        return toResponse(week);
    }

    @Transactional
    public void softDelete(Long id) {
        Week week = getWeekOrThrow(id);
        week.softDelete();
        weekRepository.save(week);
    }

    @Transactional
    public void restore(Long id) {
        Week week = weekRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("WEEK_NOT_FOUND", "الأسبوع غير موجود"));

        if (!week.isDeleted()) {
            throw AppException.badRequest("NOT_DELETED", "هذا الأسبوع غير محذوف");
        }

        week.restore();
        weekRepository.save(week);
    }

    /**
     * Checks whether a week is locked for a specific user.
     * GENERAL_ADMIN bypasses all locks.
     */
    public boolean isWeekLockedForUser(Week week, UserPrincipal principal) {
        if (principal != null && principal.isAdmin()) {
            return false;
        }
        return DateUtil.isWeekLocked(week.getEndDate(), appProperties.weekLockDays(), appProperties.timeZone());
    }

    // ── Internal Helpers ─────────────────────────────────────────────

    public Week getWeekOrThrow(Long id) {
        return weekRepository.findById(id)
                .filter(w -> !w.isDeleted())
                .orElseThrow(() -> AppException.notFound("WEEK_NOT_FOUND", "الأسبوع غير موجود"));
    }

    private WeekResponse toResponse(Week week) {
        boolean locked = DateUtil.isWeekLocked(
                week.getEndDate(),
                appProperties.weekLockDays(),
                appProperties.timeZone());

        return new WeekResponse(
                week.getId(),
                week.getStartDate(),
                week.getEndDate(),
                locked,
                !week.isDeleted()
        );
    }
}
