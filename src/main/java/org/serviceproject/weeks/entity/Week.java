package org.serviceproject.weeks.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.serviceproject.common.entity.BaseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * An independent calendar period representing a church service week (Friday 00:00 → Thursday 23:59:59).
 * <p>
 * Locking is NOT stored as a boolean column; it is computed dynamically based on:
 * {@code endDate + lockDays < today}.
 * Soft-delete supported via {@link #deletedAt}.
 */
@Entity
@Table(name = "week")
@Getter
@Setter
@NoArgsConstructor
public class Week extends BaseEntity {

    @Column(name = "start_date", nullable = false, unique = true)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public Week(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
    }
}
