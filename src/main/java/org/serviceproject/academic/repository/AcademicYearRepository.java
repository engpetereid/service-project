package org.serviceproject.academic.repository;

import org.serviceproject.academic.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

    Optional<AcademicYear> findByCurrentTrue();

    Optional<AcademicYear> findByName(String name);

    boolean existsByName(String name);

    List<AcademicYear> findAllByOrderByStartDateDesc();

    /** Set all academic years to non-current. Used before marking the new one as current. */
    @Modifying
    @Query("UPDATE AcademicYear a SET a.current = false WHERE a.current = true")
    void clearCurrentFlags();
}
