package org.serviceproject.academic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.serviceproject.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * Tracks an execution of automatic student promotion and staff forward-copying
 * for a specific {@link AcademicYear}.
 * <p>
 * Ensures idempotency: only one completed run is allowed per academic year.
 */
@Entity
@Table(name = "promotion_run")
@Getter
@Setter
@NoArgsConstructor
public class PromotionRun extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false, unique = true)
    private AcademicYear academicYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromotionStatus status = PromotionStatus.IN_PROGRESS;

    @Column(name = "promoted_count", nullable = false)
    private int promotedCount = 0;

    @Column(name = "graduated_count", nullable = false)
    private int graduatedCount = 0;

    @Column(name = "skipped_count", nullable = false)
    private int skippedCount = 0;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public PromotionRun(AcademicYear academicYear) {
        this.academicYear = academicYear;
        this.status = PromotionStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void markCompleted(int promoted, int graduated, int skipped) {
        this.status = PromotionStatus.COMPLETED;
        this.promotedCount = promoted;
        this.graduatedCount = graduated;
        this.skippedCount = skipped;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = PromotionStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }
}
