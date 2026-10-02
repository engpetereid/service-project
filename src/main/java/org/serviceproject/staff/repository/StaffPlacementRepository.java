package org.serviceproject.staff.repository;

import org.serviceproject.staff.entity.StaffPlacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffPlacementRepository extends JpaRepository<StaffPlacement, Long> {

    @Query("SELECT sp FROM StaffPlacement sp " +
           "JOIN FETCH sp.person p " +
           "JOIN FETCH sp.academicYear ay " +
           "LEFT JOIN FETCH sp.ministry m " +
           "LEFT JOIN FETCH sp.gradeClass gc " +
           "WHERE sp.person.id = :personId AND sp.academicYear.id = :yearId AND p.deletedAt IS NULL")
    Optional<StaffPlacement> findByPersonIdAndAcademicYearId(@Param("personId") Long personId, @Param("yearId") Long yearId);

    @Query("SELECT sp FROM StaffPlacement sp " +
           "JOIN FETCH sp.person p " +
           "JOIN FETCH sp.academicYear ay " +
           "LEFT JOIN FETCH sp.ministry m " +
           "LEFT JOIN FETCH sp.gradeClass gc " +
           "WHERE sp.person.id = :personId AND sp.academicYear.id = :yearId")
    Optional<StaffPlacement> findAnyByPersonIdAndAcademicYearId(@Param("personId") Long personId, @Param("yearId") Long yearId);

    @Query("SELECT sp FROM StaffPlacement sp " +
           "JOIN FETCH sp.person p " +
           "JOIN FETCH sp.academicYear ay " +
           "LEFT JOIN FETCH sp.ministry m " +
           "LEFT JOIN FETCH sp.gradeClass gc " +
           "WHERE sp.academicYear.id = :yearId AND p.deletedAt IS NULL " +
           "ORDER BY m.name ASC, gc.sortOrder ASC, p.fullName ASC")
    List<StaffPlacement> findAllActiveByAcademicYearId(@Param("yearId") Long yearId);

    @Query("SELECT sp FROM StaffPlacement sp " +
           "JOIN FETCH sp.person p " +
           "JOIN FETCH sp.academicYear ay " +
           "JOIN FETCH sp.ministry m " +
           "JOIN FETCH sp.gradeClass gc " +
           "WHERE sp.academicYear.id = :yearId AND sp.ministry.id = :ministryId AND p.deletedAt IS NULL " +
           "ORDER BY gc.sortOrder ASC, p.fullName ASC")
    List<StaffPlacement> findAllActiveByAcademicYearIdAndMinistryId(@Param("yearId") Long yearId, @Param("ministryId") Long ministryId);

    @Query("SELECT sp FROM StaffPlacement sp " +
           "JOIN FETCH sp.person p " +
           "JOIN FETCH sp.academicYear ay " +
           "JOIN FETCH sp.ministry m " +
           "JOIN FETCH sp.gradeClass gc " +
           "WHERE sp.academicYear.id = :yearId AND sp.gradeClass.id = :classId AND p.deletedAt IS NULL " +
           "ORDER BY p.fullName ASC")
    List<StaffPlacement> findAllActiveByAcademicYearIdAndClassId(@Param("yearId") Long yearId, @Param("classId") Long classId);

    boolean existsByPersonIdAndAcademicYearId(Long personId, Long yearId);

    List<StaffPlacement> findAllByAcademicYearId(Long yearId);

    @Query("SELECT sp.ministry.id, COUNT(sp) FROM StaffPlacement sp WHERE sp.academicYear.id = :yearId AND sp.person.deletedAt IS NULL GROUP BY sp.ministry.id")
    List<Object[]> countActiveServantsPerMinistry(@Param("yearId") Long yearId);

    @Query("SELECT sp.gradeClass.id, COUNT(sp) FROM StaffPlacement sp WHERE sp.academicYear.id = :yearId AND sp.person.deletedAt IS NULL GROUP BY sp.gradeClass.id")
    List<Object[]> countActiveServantsPerClass(@Param("yearId") Long yearId);
}
