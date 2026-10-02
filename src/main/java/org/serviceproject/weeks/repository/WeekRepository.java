package org.serviceproject.weeks.repository;

import org.serviceproject.weeks.entity.Week;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeekRepository extends JpaRepository<Week, Long> {

    Optional<Week> findByStartDateAndDeletedAtIsNull(LocalDate startDate);

    Optional<Week> findByStartDate(LocalDate startDate);

    List<Week> findAllByDeletedAtIsNullOrderByStartDateDesc();

    List<Week> findAllByOrderByStartDateDesc();

    @Query("SELECT w FROM Week w WHERE :date BETWEEN w.startDate AND w.endDate AND w.deletedAt IS NULL")
    Optional<Week> findWeekContainingDate(@Param("date") LocalDate date);

    boolean existsByStartDate(LocalDate startDate);
}
