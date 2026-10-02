package org.serviceproject.ministries.repository;

import org.serviceproject.ministries.entity.Ministry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MinistryRepository extends JpaRepository<Ministry, Long> {

    List<Ministry> findAllByActiveTrue();

    List<Ministry> findAllByOrderByNameAsc();

    Optional<Ministry> findByName(String name);

    boolean existsByName(String name);
}
