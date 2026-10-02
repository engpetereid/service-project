package org.serviceproject.visits.entity;

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
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.weeks.entity.Week;

import java.time.LocalDateTime;

/**
 * Record of a weekly visit or call to a student.
 * <p>
 * Exactly one record per (student, week).
 * Ministry, class, and servant are captured as immutable snapshots at record time
 * to preserve historical integrity for reports and statistics.
 */
@Entity
@Table(name = "visit_record")
@Getter
@Setter
@NoArgsConstructor
public class VisitRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Person student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "week_id", nullable = false)
    private Week week;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ministry_snap_id", nullable = false)
    private Ministry ministrySnap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_snap_id", nullable = false)
    private GradeClass classSnap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servant_snap_id")
    private Person servantSnap;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VisitMethod method;

    @Column(name = "prayer_score")
    private Integer prayerScore;

    @Column(name = "reading_score")
    private Integer readingScore;

    @Column(name = "note_score")
    private Integer noteScore;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by", nullable = false)
    private UserAccount recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    public VisitRecord(Person student, Week week, AcademicYear academicYear,
                       Ministry ministrySnap, GradeClass classSnap, Person servantSnap,
                       VisitMethod method, Integer prayerScore, Integer readingScore,
                       Integer noteScore, String notes, UserAccount recordedBy) {
        this.student = student;
        this.week = week;
        this.academicYear = academicYear;
        this.ministrySnap = ministrySnap;
        this.classSnap = classSnap;
        this.servantSnap = servantSnap;
        this.method = method;
        this.prayerScore = prayerScore;
        this.readingScore = readingScore;
        this.noteScore = noteScore;
        this.notes = notes;
        this.recordedBy = recordedBy;
        this.recordedAt = LocalDateTime.now();
    }
}
