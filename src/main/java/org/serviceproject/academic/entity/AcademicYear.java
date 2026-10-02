package org.serviceproject.academic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.serviceproject.common.entity.BaseEntity;

import java.time.LocalDate;

/**
 * Academic year (e.g., "2026/2027", Sep 1 → Aug 31).
 * <p>
 * Auto-created by the scheduler on September 1.
 * Cannot be manually created or deleted.
 * Only one academic year can be {@code current=true} at a time.
 */
@Entity
@Table(name = "academic_year")
@Getter
@Setter
@NoArgsConstructor
public class AcademicYear extends BaseEntity {

    /** Display name, e.g. "2026/2027". */
    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean current = false;

    public AcademicYear(String name, LocalDate startDate, LocalDate endDate, boolean current) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.current = current;
    }
}
