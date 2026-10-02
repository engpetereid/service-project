package org.serviceproject.classes.repository;

import org.serviceproject.classes.entity.GradeClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradeClassRepository extends JpaRepository<GradeClass, Long> {

    List<GradeClass> findAllByMinistryIdAndActiveTrueOrderBySortOrderAsc(Long ministryId);

    List<GradeClass> findAllByMinistryIdOrderBySortOrderAsc(Long ministryId);

    List<GradeClass> findAllByActiveTrueOrderBySortOrderAsc();

    boolean existsByMinistryIdAndName(Long ministryId, String name);

    @Query("SELECT gc FROM GradeClass gc JOIN FETCH gc.ministry WHERE gc.id = :id")
    java.util.Optional<GradeClass> findByIdWithMinistry(@Param("id") Long id);

    @Query("SELECT gc.ministry.id, COUNT(gc) FROM GradeClass gc WHERE gc.active = true GROUP BY gc.ministry.id")
    List<Object[]> countActiveClassesPerMinistry();
}
