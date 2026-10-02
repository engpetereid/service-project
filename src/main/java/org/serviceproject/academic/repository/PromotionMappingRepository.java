package org.serviceproject.academic.repository;

import org.serviceproject.academic.entity.PromotionMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionMappingRepository extends JpaRepository<PromotionMapping, Long> {

    Optional<PromotionMapping> findBySourceClassId(Long sourceClassId);

    @Query("SELECT pm FROM PromotionMapping pm " +
           "JOIN FETCH pm.sourceClass sc " +
           "LEFT JOIN FETCH pm.targetClass tc " +
           "ORDER BY sc.sortOrder")
    List<PromotionMapping> findAllWithClasses();

    boolean existsBySourceClassId(Long sourceClassId);
}
