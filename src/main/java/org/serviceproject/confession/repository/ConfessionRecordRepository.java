package org.serviceproject.confession.repository;

import org.serviceproject.confession.entity.ConfessionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConfessionRecordRepository extends JpaRepository<ConfessionRecord, Long> {

    @Query("SELECT cr FROM ConfessionRecord cr " +
           "JOIN FETCH cr.student s " +
           "JOIN FETCH cr.academicYear ay " +
           "JOIN FETCH cr.recordedBy rb " +
           "JOIN FETCH rb.person rbp " +
           "WHERE s.id = :studentId " +
           "ORDER BY cr.confessionDate DESC")
    List<ConfessionRecord> findAllByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT cr FROM ConfessionRecord cr " +
           "JOIN FETCH cr.student s " +
           "JOIN FETCH cr.academicYear ay " +
           "JOIN FETCH cr.recordedBy rb " +
           "JOIN FETCH rb.person rbp " +
           "WHERE s.id = :studentId AND ay.id = :yearId " +
           "ORDER BY cr.confessionDate DESC")
    List<ConfessionRecord> findAllByStudentIdAndAcademicYearId(@Param("studentId") Long studentId, @Param("yearId") Long yearId);

    @Query("SELECT cr FROM ConfessionRecord cr " +
           "JOIN FETCH cr.student s " +
           "JOIN FETCH cr.academicYear ay " +
           "JOIN FETCH cr.recordedBy rb " +
           "JOIN FETCH rb.person rbp " +
           "WHERE ay.id = :yearId " +
           "ORDER BY cr.confessionDate DESC, s.fullName ASC")
    List<ConfessionRecord> findAllByAcademicYearId(@Param("yearId") Long yearId);

    @Query("SELECT cr FROM ConfessionRecord cr " +
           "JOIN FETCH cr.student s " +
           "JOIN FETCH cr.academicYear ay " +
           "JOIN FETCH cr.recordedBy rb " +
           "JOIN FETCH rb.person rbp " +
           "WHERE cr.id = :id")
    Optional<ConfessionRecord> findByIdWithDetails(@Param("id") Long id);

    boolean existsByStudentIdAndConfessionDate(Long studentId, LocalDate confessionDate);

    Optional<ConfessionRecord> findByStudentIdAndConfessionDate(Long studentId, LocalDate confessionDate);
}
