package org.serviceproject.attendance.repository;

import org.serviceproject.attendance.entity.ActivityType;
import org.serviceproject.attendance.entity.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

    @Query("SELECT s FROM AttendanceSession s JOIN FETCH s.week w WHERE w.id = :weekId ORDER BY s.sessionDate ASC, s.activityType ASC")
    List<AttendanceSession> findAllByWeekId(@Param("weekId") Long weekId);

    @Query("SELECT s FROM AttendanceSession s JOIN FETCH s.week w WHERE s.id = :id")
    Optional<AttendanceSession> findByIdWithWeek(@Param("id") Long id);

    boolean existsByWeekIdAndActivityTypeAndSessionDate(Long weekId, ActivityType activityType, LocalDate sessionDate);
}
