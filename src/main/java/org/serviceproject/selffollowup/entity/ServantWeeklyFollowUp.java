package org.serviceproject.selffollowup.entity;

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
import org.serviceproject.users.entity.UserAccount;
import org.serviceproject.weeks.entity.Week;

/**
 * Record of a servant's or class secretary's personal weekly self-follow-up.
 * <p>
 * Exactly one record per (user, week).
 * Tracks spiritual discipline (Mass, Service Meeting, Tasbeha, Management Meeting, Note Score).
 * {@code maxNoteScoreSnapshot} captures the system setting at record creation time
 * to protect historical percentage accuracy.
 */
@Entity
@Table(name = "servant_weekly_follow_up")
@Getter
@Setter
@NoArgsConstructor
public class ServantWeeklyFollowUp extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "week_id", nullable = false)
    private Week week;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(name = "note_score")
    private Integer noteScore;

    @Column(name = "max_note_score_snapshot", nullable = false)
    private Integer maxNoteScoreSnapshot;

    @Column(name = "attended_mass")
    private Boolean attendedMass;

    @Column(name = "attended_service_meeting")
    private Boolean attendedServiceMeeting;

    @Column(name = "attended_tasbeha")
    private Boolean attendedTasbeha;

    @Column(name = "attended_management_meeting")
    private Boolean attendedManagementMeeting;

    public ServantWeeklyFollowUp(UserAccount user, Week week, AcademicYear academicYear,
                                 Integer noteScore, Integer maxNoteScoreSnapshot,
                                 Boolean attendedMass, Boolean attendedServiceMeeting,
                                 Boolean attendedTasbeha, Boolean attendedManagementMeeting) {
        this.user = user;
        this.week = week;
        this.academicYear = academicYear;
        this.noteScore = noteScore;
        this.maxNoteScoreSnapshot = maxNoteScoreSnapshot;
        this.attendedMass = attendedMass;
        this.attendedServiceMeeting = attendedServiceMeeting;
        this.attendedTasbeha = attendedTasbeha;
        this.attendedManagementMeeting = attendedManagementMeeting;
    }
}
