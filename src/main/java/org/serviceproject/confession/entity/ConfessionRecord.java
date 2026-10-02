package org.serviceproject.confession.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.common.entity.BaseEntity;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.UserAccount;

import java.time.LocalDate;

/**
 * Record of a student's confession.
 * <p>
 * Linked directly to {@link Person} (student) and {@link AcademicYear}.
 * Not tied to Week cycles.
 */
@Entity
@Table(name = "confession_record")
@Getter
@Setter
@NoArgsConstructor
public class ConfessionRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Person student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(name = "confession_date", nullable = false)
    private LocalDate confessionDate;

    @Column(name = "confession_father", length = 100)
    private String confessionFather;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by", nullable = false)
    private UserAccount recordedBy;

    public ConfessionRecord(Person student, AcademicYear academicYear, LocalDate confessionDate,
                            String confessionFather, String notes, UserAccount recordedBy) {
        this.student = student;
        this.academicYear = academicYear;
        this.confessionDate = confessionDate;
        this.confessionFather = confessionFather;
        this.notes = notes;
        this.recordedBy = recordedBy;
    }
}
