package org.serviceproject.students.repository;

import org.serviceproject.students.entity.StudentPlacement;
import org.serviceproject.students.entity.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentPlacementRepository extends JpaRepository<StudentPlacement, Long> {

    @Query("SELECT sp FROM StudentPlacement sp " +
           "JOIN FETCH sp.person p " +
           "JOIN FETCH sp.academicYear ay " +
           "LEFT JOIN FETCH sp.ministry m " +
           "LEFT JOIN FETCH sp.gradeClass gc " +
           "LEFT JOIN FETCH sp.responsibleServant s " +
           "WHERE sp.person.id = :personId AND sp.academicYear.id = :yearId AND p.deletedAt IS NULL")
    Optional<StudentPlacement> findByPersonIdAndAcademicYearId(@Param("personId") Long personId, @Param("yearId") Long yearId);

    @Query("SELECT sp FROM StudentPlacement sp " +
           "JOIN FETCH sp.person p " +
           "JOIN FETCH sp.academicYear ay " +
           "LEFT JOIN FETCH sp.ministry m " +
           "LEFT JOIN FETCH sp.gradeClass gc " +
           "LEFT JOIN FETCH sp.responsibleServant s " +
           "WHERE sp.person.id = :personId AND sp.academicYear.id = :yearId")
    Optional<StudentPlacement> findAnyByPersonIdAndAcademicYearId(@Param("personId") Long personId, @Param("yearId") Long yearId);

    @Query("SELECT sp FROM StudentPlacement sp " +
           "JOIN FETCH sp.person p " +
           "JOIN FETCH sp.academicYear ay " +
           "LEFT JOIN FETCH sp.ministry m " +
           "LEFT JOIN FETCH sp.gradeClass gc " +
           "LEFT JOIN FETCH sp.responsibleServant s " +
           "WHERE sp.academicYear.id = :yearId AND sp.status = :status AND p.deletedAt IS NULL " +
           "ORDER BY m.name ASC, gc.sortOrder ASC, p.fullName ASC")
    List<StudentPlacement> findAllByAcademicYearIdAndStatus(
            @Param("yearId") Long yearId,
            @Param("status") StudentStatus status);

    @Query("SELECT sp FROM StudentPlacement sp " +
           "JOIN FETCH sp.person p " +
           "JOIN FETCH sp.academicYear ay " +
           "LEFT JOIN FETCH sp.ministry m " +
           "LEFT JOIN FETCH sp.gradeClass gc " +
           "LEFT JOIN FETCH sp.responsibleServant s " +
           "WHERE sp.academicYear.id = :yearId AND sp.ministry.id = :ministryId AND sp.status = :status AND p.deletedAt IS NULL " +
           "ORDER BY gc.sortOrder ASC, p.fullName ASC")
    List<StudentPlacement> findAllByAcademicYearIdAndMinistryIdAndStatus(
            @Param("yearId") Long yearId,
            @Param("ministryId") Long ministryId,
            @Param("status") StudentStatus status);

    @Query("SELECT sp FROM StudentPlacement sp " +
           "JOIN FETCH sp.person p " +
           "JOIN FETCH sp.academicYear ay " +
           "LEFT JOIN FETCH sp.ministry m " +
           "LEFT JOIN FETCH sp.gradeClass gc " +
           "LEFT JOIN FETCH sp.responsibleServant s " +
           "WHERE sp.academicYear.id = :yearId AND sp.gradeClass.id = :classId AND sp.status = :status AND p.deletedAt IS NULL " +
           "ORDER BY p.fullName ASC")
    List<StudentPlacement> findAllByAcademicYearIdAndClassIdAndStatus(
            @Param("yearId") Long yearId,
            @Param("classId") Long classId,
            @Param("status") StudentStatus status);

    @Query("SELECT sp FROM StudentPlacement sp " +
           "JOIN FETCH sp.person p " +
           "JOIN FETCH sp.academicYear ay " +
           "LEFT JOIN FETCH sp.ministry m " +
           "LEFT JOIN FETCH sp.gradeClass gc " +
           "LEFT JOIN FETCH sp.responsibleServant s " +
           "WHERE sp.academicYear.id = :yearId AND sp.responsibleServant.id = :servantId AND sp.status = :status AND p.deletedAt IS NULL " +
           "ORDER BY p.fullName ASC")
    List<StudentPlacement> findAllByAcademicYearIdAndServantIdAndStatus(
            @Param("yearId") Long yearId,
            @Param("servantId") Long servantId,
            @Param("status") StudentStatus status);

    @Query("SELECT sp FROM StudentPlacement sp " +
           "JOIN FETCH sp.person p " +
           "JOIN FETCH sp.academicYear ay " +
           "LEFT JOIN FETCH sp.ministry m " +
           "LEFT JOIN FETCH sp.gradeClass gc " +
           "LEFT JOIN FETCH sp.responsibleServant s " +
           "WHERE sp.academicYear.id = :yearId AND sp.status = :status AND p.deletedAt IS NULL " +
           "AND (LOWER(p.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR p.phone LIKE CONCAT('%', :search, '%')) " +
           "ORDER BY p.fullName ASC")
    List<StudentPlacement> searchByAcademicYearIdAndStatus(
            @Param("yearId") Long yearId,
            @Param("search") String search,
            @Param("status") StudentStatus status);

    boolean existsByPersonIdAndAcademicYearId(Long personId, Long yearId);

    List<StudentPlacement> findAllByAcademicYearId(Long yearId);

    @Query("SELECT sp.ministry.id, COUNT(sp) FROM StudentPlacement sp WHERE sp.academicYear.id = :yearId AND sp.status = org.serviceproject.students.entity.StudentStatus.ACTIVE AND sp.person.deletedAt IS NULL GROUP BY sp.ministry.id")
    List<Object[]> countActiveStudentsPerMinistry(@Param("yearId") Long yearId);

    @Query("SELECT sp.gradeClass.id, COUNT(sp) FROM StudentPlacement sp WHERE sp.academicYear.id = :yearId AND sp.status = org.serviceproject.students.entity.StudentStatus.ACTIVE AND sp.person.deletedAt IS NULL GROUP BY sp.gradeClass.id")
    List<Object[]> countActiveStudentsPerClass(@Param("yearId") Long yearId);
}
