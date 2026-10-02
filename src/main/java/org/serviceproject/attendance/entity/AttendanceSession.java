package org.serviceproject.attendance.entity;

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
import org.serviceproject.common.entity.BaseEntity;
import org.serviceproject.weeks.entity.Week;

import java.time.LocalDate;

/**
 * An attendance event tied to a specific {@link Week}, {@link ActivityType}, and date.
 */
@Entity
@Table(name = "attendance_session")
@Getter
@Setter
@NoArgsConstructor
public class AttendanceSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "week_id", nullable = false)
    private Week week;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 20)
    private ActivityType activityType;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    public AttendanceSession(Week week, ActivityType activityType, LocalDate sessionDate) {
        this.week = week;
        this.activityType = activityType;
        this.sessionDate = sessionDate;
    }
}
