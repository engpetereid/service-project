package org.serviceproject.attendance.repository;

import org.serviceproject.attendance.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    @Query("SELECT ar FROM AttendanceRecord ar " +
           "JOIN FETCH ar.session s " +
           "JOIN FETCH ar.student st " +
           "JOIN FETCH ar.recordedBy rb " +
           "JOIN FETCH rb.person rbp " +
           "WHERE s.id = :sessionId " +
           "ORDER BY st.fullName ASC")
    List<AttendanceRecord> findAllBySessionId(@Param("sessionId") Long sessionId);

    Optional<AttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);

    boolean existsBySessionIdAndStudentId(Long sessionId, Long studentId);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.session.id = :sessionId AND ar.present = true")
    long countPresentBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT ar FROM AttendanceRecord ar " +
           "JOIN FETCH ar.session s " +
           "WHERE s.week.id = :weekId AND s.activityType = :activityType AND ar.present = true")
    List<AttendanceRecord> findAllPresentByWeekIdAndActivityType(
            @Param("weekId") Long weekId,
            @Param("activityType") org.serviceproject.attendance.entity.ActivityType activityType);

    @Query("SELECT ar FROM AttendanceRecord ar " +
           "JOIN FETCH ar.session s " +
           "WHERE ar.student.id = :studentId AND s.week.id IN :weekIds AND ar.present = true")
    List<AttendanceRecord> findAllPresentByStudentIdAndWeekIds(
            @Param("studentId") Long studentId,
            @Param("weekIds") List<Long> weekIds);

    @Query("SELECT ar FROM AttendanceRecord ar " +
           "JOIN FETCH ar.session s " +
           "WHERE ar.student.id IN :studentIds AND s.week.id IN :weekIds AND ar.present = true")
    List<AttendanceRecord> findAllPresentByStudentIdsAndWeekIds(
            @Param("studentIds") List<Long> studentIds,
            @Param("weekIds") List<Long> weekIds);

    @Query("SELECT ar FROM AttendanceRecord ar " +
           "JOIN FETCH ar.session s " +
           "WHERE ar.student.id = :studentId AND ar.present = true")
    List<AttendanceRecord> findAllPresentByStudentId(@Param("studentId") Long studentId);
}
