package org.serviceproject.selffollowup.repository;

import org.serviceproject.selffollowup.entity.ServantWeeklyFollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServantWeeklyFollowUpRepository extends JpaRepository<ServantWeeklyFollowUp, Long> {

    @Query("SELECT f FROM ServantWeeklyFollowUp f " +
           "JOIN FETCH f.week w " +
           "JOIN FETCH f.academicYear ay " +
           "JOIN FETCH f.user u " +
           "WHERE f.user.id = :userId AND f.week.id = :weekId")
    Optional<ServantWeeklyFollowUp> findByUserIdAndWeekId(
            @Param("userId") Long userId,
            @Param("weekId") Long weekId);

    @Query("SELECT f FROM ServantWeeklyFollowUp f " +
           "JOIN FETCH f.week w " +
           "JOIN FETCH f.academicYear ay " +
           "JOIN FETCH f.user u " +
           "WHERE f.id = :id")
    Optional<ServantWeeklyFollowUp> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT f FROM ServantWeeklyFollowUp f " +
           "JOIN FETCH f.week w " +
           "JOIN FETCH f.academicYear ay " +
           "WHERE f.user.id = :userId AND f.academicYear.id = :academicYearId " +
           "ORDER BY w.startDate DESC")
    List<ServantWeeklyFollowUp> findAllByUserIdAndAcademicYearIdOrderByWeekStartDateDesc(
            @Param("userId") Long userId,
            @Param("academicYearId") Long academicYearId);

    @Query("SELECT f FROM ServantWeeklyFollowUp f " +
           "JOIN FETCH f.week w " +
           "JOIN FETCH f.academicYear ay " +
           "WHERE f.user.id = :userId AND f.academicYear.id = :academicYearId " +
           "ORDER BY w.startDate ASC")
    List<ServantWeeklyFollowUp> findAllByUserIdAndAcademicYearIdOrderByWeekStartDateAsc(
            @Param("userId") Long userId,
            @Param("academicYearId") Long academicYearId);

    boolean existsByUserIdAndWeekId(Long userId, Long weekId);

    @Query("SELECT f FROM ServantWeeklyFollowUp f " +
           "JOIN FETCH f.week w " +
           "JOIN FETCH f.academicYear ay " +
           "JOIN FETCH f.user u " +
           "JOIN FETCH u.person p " +
           "WHERE f.week.id = :weekId")
    List<ServantWeeklyFollowUp> findAllByWeekId(@Param("weekId") Long weekId);

    @Query("SELECT COUNT(f) FROM ServantWeeklyFollowUp f " +
           "WHERE f.user.id = :userId AND f.academicYear.id = :academicYearId")
    long countByUserIdAndAcademicYearId(
            @Param("userId") Long userId,
            @Param("academicYearId") Long academicYearId);
}
