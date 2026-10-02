package org.serviceproject.academic.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.serviceproject.academic.dto.PromotionRunResponse;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.academic.entity.PromotionMapping;
import org.serviceproject.academic.entity.PromotionRun;
import org.serviceproject.academic.entity.PromotionStatus;
import org.serviceproject.academic.repository.AcademicYearRepository;
import org.serviceproject.academic.repository.PromotionMappingRepository;
import org.serviceproject.academic.repository.PromotionRunRepository;
import org.serviceproject.classes.entity.GradeClass;
import org.serviceproject.common.exception.AppException;
import org.serviceproject.staff.entity.StaffPlacement;
import org.serviceproject.staff.repository.StaffPlacementRepository;
import org.serviceproject.students.entity.StudentPlacement;
import org.serviceproject.students.entity.StudentStatus;
import org.serviceproject.students.repository.StudentPlacementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service managing student promotion runs across academic years.
 * <p>
 * Enforces idempotency via {@link PromotionRun}. Automatically handles:
 * 1. Promoting students to target classes based on {@link PromotionMapping}.
 * 2. Graduating students where mapping specifies graduation.
 * 3. Copying staff placements forward for organizational continuity.
 * 4. Skipping students with unmapped classes safely with warnings.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRunRepository promotionRunRepository;
    private final PromotionMappingRepository promotionMappingRepository;
    private final AcademicYearRepository academicYearRepository;
    private final StudentPlacementRepository studentPlacementRepository;
    private final StaffPlacementRepository staffPlacementRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<PromotionRunResponse> getHistory() {
        return promotionRunRepository.findAllOrderByStartedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PromotionRunResponse getRunForYear(Long academicYearId) {
        PromotionRun run = promotionRunRepository.findByAcademicYearId(academicYearId)
                .orElseThrow(() -> AppException.notFound("PROMOTION_RUN_NOT_FOUND", "لا يوجد سجل ترفيع لهذا العام"));
        return toResponse(run);
    }

    /**
     * Executes promotion for the specified target academic year.
     * Idempotent: returns existing run if already completed.
     */
    @Transactional
    public PromotionRunResponse executePromotion(Long targetYearId) {
        AcademicYear targetYear = academicYearRepository.findById(targetYearId)
                .orElseThrow(() -> AppException.notFound("YEAR_NOT_FOUND", "العام الدراسي غير موجود"));

        return executePromotion(targetYear);
    }

    /**
     * Core promotion execution method.
     */
    @Transactional
    public PromotionRunResponse executePromotion(AcademicYear targetYear) {
        Optional<PromotionRun> existingRunOpt = promotionRunRepository.findByAcademicYearId(targetYear.getId());
        if (existingRunOpt.isPresent()) {
            PromotionRun existing = existingRunOpt.get();
            if (existing.getStatus() == PromotionStatus.COMPLETED) {
                log.info("Promotion already completed for year: {}. Skipping.", targetYear.getName());
                return toResponse(existing);
            }
        }

        // Find previous academic year (immediately preceding targetYear)
        Optional<AcademicYear> previousYearOpt = academicYearRepository.findAllByOrderByStartDateDesc().stream()
                .filter(y -> y.getStartDate().isBefore(targetYear.getStartDate()))
                .findFirst();

        PromotionRun run = existingRunOpt.orElseGet(() -> new PromotionRun(targetYear));
        run.setStatus(PromotionStatus.IN_PROGRESS);
        run = promotionRunRepository.save(run);

        if (previousYearOpt.isEmpty()) {
            log.info("No previous academic year found prior to {}. Marking promotion run as completed with 0 counts.", targetYear.getName());
            run.markCompleted(0, 0, 0);
            run = promotionRunRepository.save(run);
            return toResponse(run);
        }

        AcademicYear previousYear = previousYearOpt.get();
        log.info("Starting promotion from year {} to year {}", previousYear.getName(), targetYear.getName());

        int promotedCount = 0;
        int graduatedCount = 0;
        int skippedCount = 0;

        try {
            // 1. Promote active students of the previous year
            List<StudentPlacement> previousStudentPlacements =
                    studentPlacementRepository.findAllByAcademicYearIdAndStatus(previousYear.getId(), StudentStatus.ACTIVE);

            for (StudentPlacement prevPlacement : previousStudentPlacements) {
                // If student is already placed in target year, skip
                if (studentPlacementRepository.existsByPersonIdAndAcademicYearId(prevPlacement.getPerson().getId(), targetYear.getId())) {
                    continue;
                }

                if (prevPlacement.getGradeClass() == null) {
                    skippedCount++;
                    continue;
                }

                Optional<PromotionMapping> mappingOpt =
                        promotionMappingRepository.findBySourceClassId(prevPlacement.getGradeClass().getId());

                if (mappingOpt.isEmpty()) {
                    log.warn("No promotion mapping found for class {} (student: {}). Skipping.",
                            prevPlacement.getGradeClass().getName(), prevPlacement.getPerson().getFullName());
                    skippedCount++;
                    continue;
                }

                PromotionMapping mapping = mappingOpt.get();

                if (mapping.isGraduation()) {
                    StudentPlacement gradPlacement = new StudentPlacement();
                    gradPlacement.setPerson(prevPlacement.getPerson());
                    gradPlacement.setAcademicYear(targetYear);
                    gradPlacement.setMinistry(null);
                    gradPlacement.setGradeClass(null);
                    gradPlacement.setResponsibleServant(null);
                    gradPlacement.setStatus(StudentStatus.GRADUATED);
                    gradPlacement.setGuardianPhone(prevPlacement.getGuardianPhone());
                    gradPlacement.setTalents(prevPlacement.getTalents());
                    gradPlacement.setAdditionalDetails(prevPlacement.getAdditionalDetails());

                    studentPlacementRepository.save(gradPlacement);
                    graduatedCount++;
                } else {
                    GradeClass targetClass = mapping.getTargetClass();
                    if (targetClass == null) {
                        log.warn("Mapping for class {} has null targetClass and is not graduation. Skipping.",
                                prevPlacement.getGradeClass().getName());
                        skippedCount++;
                        continue;
                    }

                    StudentPlacement newPlacement = new StudentPlacement(
                            prevPlacement.getPerson(),
                            targetYear,
                            targetClass.getMinistry(),
                            targetClass
                    );
                    newPlacement.setStatus(StudentStatus.ACTIVE);
                    newPlacement.setResponsibleServant(null); // servant assignment is reset on promotion
                    newPlacement.setGuardianPhone(prevPlacement.getGuardianPhone());
                    newPlacement.setTalents(prevPlacement.getTalents());
                    newPlacement.setAdditionalDetails(prevPlacement.getAdditionalDetails());

                    studentPlacementRepository.save(newPlacement);
                    promotedCount++;
                }
            }

            // 2. Copy staff placements forward to new year for organizational continuity
            List<StaffPlacement> previousStaffPlacements =
                    staffPlacementRepository.findAllByAcademicYearId(previousYear.getId());

            for (StaffPlacement prevStaff : previousStaffPlacements) {
                if (!staffPlacementRepository.existsByPersonIdAndAcademicYearId(prevStaff.getPerson().getId(), targetYear.getId())) {
                    StaffPlacement newStaff = new StaffPlacement(
                            prevStaff.getPerson(),
                            targetYear,
                            prevStaff.getMinistry(),
                            prevStaff.getGradeClass()
                    );
                    staffPlacementRepository.save(newStaff);
                }
            }

            run.markCompleted(promotedCount, graduatedCount, skippedCount);
            run = promotionRunRepository.save(run);
            log.info("Promotion completed successfully for year {}: {} promoted, {} graduated, {} skipped.",
                    targetYear.getName(), promotedCount, graduatedCount, skippedCount);

            if (eventPublisher != null) {
                eventPublisher.publishEvent(org.serviceproject.audit.event.AuditEvent.system(
                        org.serviceproject.audit.entity.AuditAction.PROMOTION,
                        "AcademicYear",
                        targetYear.getId(),
                        null,
                        "Promoted: " + promotedCount + ", Graduated: " + graduatedCount + ", Skipped: " + skippedCount
                ));
            }

        } catch (Exception ex) {
            log.error("Promotion failed for year: {}", targetYear.getName(), ex);
            run.markFailed();
            promotionRunRepository.save(run);
            throw AppException.badRequest("PROMOTION_FAILED", "حدث خطأ أثناء تنفيذ عملية الترفيع: " + ex.getMessage());
        }

        return toResponse(run);
    }

    private PromotionRunResponse toResponse(PromotionRun run) {
        return new PromotionRunResponse(
                run.getId(),
                run.getAcademicYear().getId(),
                run.getAcademicYear().getName(),
                run.getStatus().name(),
                run.getPromotedCount(),
                run.getGraduatedCount(),
                run.getSkippedCount(),
                run.getStartedAt(),
                run.getCompletedAt()
        );
    }
}
