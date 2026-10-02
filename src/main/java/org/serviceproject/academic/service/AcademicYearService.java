package org.serviceproject.academic.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serviceproject.academic.dto.AcademicYearResponse;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.repository.AcademicYearRepository;
import org.serviceproject.common.config.AppProperties;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.common.util.DateUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Academic year management.
 * <p>
 * Academic years are auto-created by the scheduler on September 1.
 * They cannot be manually created or deleted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public List<AcademicYearResponse> findAll() {
        return academicYearRepository.findAllByOrderByStartDateDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AcademicYearResponse findCurrent() {
        return toResponse(getCurrentEntity());
    }

    @Transactional
    public AcademicYear getCurrentEntity() {
        return academicYearRepository.findByCurrentTrue()
                .orElseGet(() -> ensureAcademicYear(DateUtil.today(appProperties.timeZone())));
    }

    /**
     * Ensure the academic year for the given date exists.
     * Called by the scheduler and on-demand fallback. Idempotent.
     *
     * @return the created or existing academic year
     */
    @Transactional
    public AcademicYear ensureAcademicYear(LocalDate date) {
        String yearName = DateUtil.academicYearName(date);

        return academicYearRepository.findByName(yearName)
                .map(year -> {
                    if (!year.isCurrent()) {
                        academicYearRepository.clearCurrentFlags();
                        year.setCurrent(true);
                        return academicYearRepository.save(year);
                    }
                    return year;
                })
                .orElseGet(() -> createAcademicYear(date, yearName));
    }

    // ── Internal ─────────────────────────────────────────────────────

    private AcademicYear createAcademicYear(LocalDate date, String yearName) {
        int startYear = DateUtil.academicStartYear(date);
        LocalDate startDate = DateUtil.academicYearStartDate(startYear);
        LocalDate endDate = DateUtil.academicYearEndDate(startYear);

        // Clear current flags before setting the new year as current
        academicYearRepository.clearCurrentFlags();

        AcademicYear year = new AcademicYear(yearName, startDate, endDate, true);
        year = academicYearRepository.save(year);

        log.info("Created academic year: {} (current=true)", yearName);
        return year;
    }

    private AcademicYearResponse toResponse(AcademicYear year) {
        return new AcademicYearResponse(
                year.getId(),
                year.getName(),
                year.getStartDate(),
                year.getEndDate(),
                year.isCurrent()
        );
    }
}
