package org.serviceproject.users.repository;

import org.serviceproject.users.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

    /** Find active (non-deleted) person by id. */
    Optional<Person> findByIdAndDeletedAtIsNull(Long id);

    /** Find active person by phone number. */
    Optional<Person> findByPhoneAndDeletedAtIsNull(String phone);

    /** Find person by phone number (active or deleted). */
    Optional<Person> findByPhone(String phone);

    /** Check if a phone number is already in use (including deleted people). */
    boolean existsByPhone(String phone);

    /** List all active (non-deleted) people. */
    List<Person> findAllByDeletedAtIsNull();

    /** List all deleted people (for archive). */
    List<Person> findAllByDeletedAtIsNotNull();

    /** Search active people by name (case-insensitive contains). */
    List<Person> findByFullNameContainingIgnoreCaseAndDeletedAtIsNull(String name);
}
