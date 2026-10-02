package org.serviceproject.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.serviceproject.common.entity.BaseEntity;
import org.serviceproject.users.entity.Person;
import org.serviceproject.users.entity.UserAccount;

import java.time.LocalDateTime;

/**
 * Attendance record marking whether a student was present at a specific {@link AttendanceSession}.
 */
@Entity
@Table(name = "attendance_record")
@Getter
@Setter
@NoArgsConstructor
public class AttendanceRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AttendanceSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Person student;

    @Column(nullable = false)
    private boolean present = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by", nullable = false)
    private UserAccount recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    public AttendanceRecord(AttendanceSession session, Person student, boolean present, UserAccount recordedBy) {
        this.session = session;
        this.student = student;
        this.present = present;
        this.recordedBy = recordedBy;
        this.recordedAt = LocalDateTime.now();
    }
}
