package org.serviceproject.staff.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.serviceproject.academic.entity.AcademicYear;
import org.serviceproject.classes.entity.GradeClass;
import org.serviceproject.common.entity.BaseEntity;
import org.serviceproject.ministries.entity.Ministry;
import org.serviceproject.users.entity.Person;

/**
 * Assigns a servant ({@link Person}) to a specific {@link Ministry} and {@link GradeClass}
 * for an {@link AcademicYear}.
 * <p>
 * A servant can have only one placement per academic year (unique constraint on person_id + academic_year_id).
 */
@Entity
@Table(name = "staff_placement")
@Getter
@Setter
@NoArgsConstructor
public class StaffPlacement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ministry_id", nullable = false)
    private Ministry ministry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private GradeClass gradeClass;

    public StaffPlacement(Person person, AcademicYear academicYear, Ministry ministry, GradeClass gradeClass) {
        this.person = person;
        this.academicYear = academicYear;
        this.ministry = ministry;
        this.gradeClass = gradeClass;
    }
}
