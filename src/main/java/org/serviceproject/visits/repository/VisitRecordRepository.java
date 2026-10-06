package org.serviceproject.visits.repository;

import org.serviceproject.visits.entity.VisitRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VisitRecordRepository extends JpaRepository<VisitRecord, Long> {

    @Query("SELECT vr FROM VisitRecord vr " +
           "JOIN FETCH vr.student s " +
           "JOIN FETCH vr.week w " +
           "JOIN FETCH vr.academicYear ay " +
           "JOIN FETCH vr.ministrySnap m " +
           "JOIN FETCH vr.classSnap gc " +
           "LEFT JOIN FETCH vr.servantSnap serv " +
           "JOIN FETCH vr.recordedBy rb " +
           "JOIN FETCH rb.person rbp " +
           "WHERE vr.student.id = :studentId AND vr.week.id = :weekId")
    Optional<VisitRecord> findByStudentIdAndWeekId(@Param("studentId") Long studentId, @Param("weekId") Long weekId);

    @Query("SELECT vr FROM VisitRecord vr " +
           "JOIN FETCH vr.student s " +
           "JOIN FETCH vr.week w " +
           "JOIN FETCH vr.academicYear ay " +
           "JOIN FETCH vr.ministrySnap m " +
           "JOIN FETCH vr.classSnap gc " +
           "LEFT JOIN FETCH vr.servantSnap serv " +
           "JOIN FETCH vr.recordedBy rb " +
           "JOIN FETCH rb.person rbp " +
           "WHERE vr.id = :id")
    Optional<VisitRecord> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT vr FROM VisitRecord vr " +
           "JOIN FETCH vr.student s " +
           "JOIN FETCH vr.week w " +
           "JOIN FETCH vr.academicYear ay " +
           "JOIN FETCH vr.ministrySnap m " +
           "JOIN FETCH vr.classSnap gc " +
           "LEFT JOIN FETCH vr.servantSnap serv " +
           "JOIN FETCH vr.recordedBy rb " +
           "JOIN FETCH rb.person rbp " +
           "WHERE vr.week.id = :weekId " +
           "ORDER BY s.fullName ASC")
    List<VisitRecord> findAllByWeekId(@Param("weekId") Long weekId);

    @Query("SELECT vr FROM VisitRecord vr " +
           "JOIN FETCH vr.student s " +
           "JOIN FETCH vr.week w " +
           "JOIN FETCH vr.academicYear ay " +
           "JOIN FETCH vr.ministrySnap m " +
           "JOIN FETCH vr.classSnap gc " +
           "LEFT JOIN FETCH vr.servantSnap serv " +
           "JOIN FETCH vr.recordedBy rb " +
           "JOIN FETCH rb.person rbp " +
           "WHERE vr.week.id = :weekId AND vr.ministrySnap.id = :ministryId " +
           "ORDER BY s.fullName ASC")
    List<VisitRecord> findAllByWeekIdAndMinistrySnapId(@Param("weekId") Long weekId, @Param("ministryId") Long ministryId);

    @Query("SELECT vr FROM VisitRecord vr " +
           "JOIN FETCH vr.student s " +
           "JOIN FETCH vr.week w " +
           "JOIN FETCH vr.academicYear ay " +
           "JOIN FETCH vr.ministrySnap m " +
           "JOIN FETCH vr.classSnap gc " +
           "LEFT JOIN FETCH vr.servantSnap serv " +
           "JOIN FETCH vr.recordedBy rb " +
           "JOIN FETCH rb.person rbp " +
           "WHERE vr.week.id = :weekId AND vr.classSnap.id = :classId " +
           "ORDER BY s.fullName ASC")
    List<VisitRecord> findAllByWeekIdAndClassSnapId(@Param("weekId") Long weekId, @Param("classId") Long classId);

    @Query("SELECT vr FROM VisitRecord vr " +
           "JOIN FETCH vr.student s " +
           "JOIN FETCH vr.week w " +
           "JOIN FETCH vr.academicYear ay " +
           "JOIN FETCH vr.ministrySnap m " +
           "JOIN FETCH vr.classSnap gc " +
           "LEFT JOIN FETCH vr.servantSnap serv " +
           "JOIN FETCH vr.recordedBy rb " +
           "JOIN FETCH rb.person rbp " +
           "WHERE vr.week.id = :weekId AND vr.servantSnap.id = :servantId " +
           "ORDER BY s.fullName ASC")
    List<VisitRecord> findAllByWeekIdAndServantSnapId(@Param("weekId") Long weekId, @Param("servantId") Long servantId);

    boolean existsByStudentIdAndWeekId(Long studentId, Long weekId);

    @Query("SELECT vr FROM VisitRecord vr " +
           "WHERE vr.student.id = :studentId AND vr.week.id IN :weekIds")
    List<VisitRecord> findAllByStudentIdAndWeekIds(
            @Param("studentId") Long studentId,
            @Param("weekIds") List<Long> weekIds);

    @Query("SELECT vr FROM VisitRecord vr " +
           "WHERE vr.student.id IN :studentIds AND vr.week.id IN :weekIds")
    List<VisitRecord> findAllByStudentIdsAndWeekIds(
            @Param("studentIds") List<Long> studentIds,
            @Param("weekIds") List<Long> weekIds);

    @Query("SELECT vr FROM VisitRecord vr " +
           "JOIN FETCH vr.student s " +
           "JOIN FETCH vr.week w " +
           "LEFT JOIN FETCH vr.servantSnap serv " +
           "WHERE vr.student.id = :studentId " +
           "ORDER BY w.startDate DESC")
    List<VisitRecord> findAllByStudentIdOrderByWeekDesc(@Param("studentId") Long studentId);
}
