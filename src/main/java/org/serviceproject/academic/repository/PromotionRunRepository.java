package org.serviceproject.academic.repository;

import org.serviceproject.academic.entity.PromotionRun;
import org.serviceproject.academic.entity.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRunRepository extends JpaRepository<PromotionRun, Long> {

    @Query("SELECT pr FROM PromotionRun pr JOIN FETCH pr.academicYear ay WHERE ay.id = :yearId")
    Optional<PromotionRun> findByAcademicYearId(@Param("yearId") Long yearId);

    boolean existsByAcademicYearIdAndStatus(Long yearId, PromotionStatus status);

    @Query("SELECT pr FROM PromotionRun pr JOIN FETCH pr.academicYear ay ORDER BY pr.startedAt DESC")
    List<PromotionRun> findAllOrderByStartedAtDesc();
}
