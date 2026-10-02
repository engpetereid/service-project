package org.serviceproject.students.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.LocalDateTime;

/**
 * Assigns a student ({@link Person}) to a specific {@link Ministry}, {@link GradeClass},
 * and optional responsible servant for an {@link AcademicYear}.
 * <p>
 * Student-specific details (guardianPhone, talents, additionalDetails) and status (ACTIVE, GRADUATED)
 * are stored per academic year placement.
 */
@Entity
@Table(name = "student_placement")
@Getter
@Setter
@NoArgsConstructor
public class StudentPlacement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ministry_id")
    private Ministry ministry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private GradeClass gradeClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servant_id")
    private Person responsibleServant;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudentStatus status = StudentStatus.ACTIVE;

    @Column(name = "guardian_phone", length = 20)
    private String guardianPhone;

    @Column(columnDefinition = "TEXT")
    private String talents;

    @Column(name = "additional_details", columnDefinition = "TEXT")
    private String additionalDetails;

    public StudentPlacement(Person person, AcademicYear academicYear, Ministry ministry, GradeClass gradeClass) {
        this.person = person;
        this.academicYear = academicYear;
        this.ministry = ministry;
        this.gradeClass = gradeClass;
        this.status = StudentStatus.ACTIVE;
    }

    public void assignServant(Person servant) {
        this.responsibleServant = servant;
        this.assignedAt = (servant != null) ? LocalDateTime.now() : null;
    }
}
