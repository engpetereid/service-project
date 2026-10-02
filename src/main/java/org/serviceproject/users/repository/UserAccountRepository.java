package org.serviceproject.users.repository;

import org.serviceproject.users.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    /**
     * Load an active user by phone with eagerly fetched person and roles.
     * Used for login authentication.
     */
    @Query("SELECT u FROM UserAccount u " +
           "JOIN FETCH u.person p " +
           "LEFT JOIN FETCH u.roles " +
           "WHERE p.phone = :phone AND p.deletedAt IS NULL AND u.enabled = true")
    Optional<UserAccount> findActiveByPhoneWithRoles(@Param("phone") String phone);

    /**
     * Load an active user by ID with eagerly fetched person and roles.
     * Used by JWT filter on every request.
     */
    @Query("SELECT u FROM UserAccount u " +
           "JOIN FETCH u.person p " +
           "LEFT JOIN FETCH u.roles " +
           "WHERE u.id = :id AND p.deletedAt IS NULL AND u.enabled = true")
    Optional<UserAccount> findActiveByIdWithRoles(@Param("id") Long id);

    /**
     * Check if any user with the GENERAL_ADMIN role exists.
     */
    @Query("SELECT CASE WHEN COUNT(ur) > 0 THEN true ELSE false END " +
           "FROM UserRole ur WHERE ur.role = org.serviceproject.users.entity.Role.GENERAL_ADMIN")
    boolean existsAnyAdmin();

    /** Find user account by person ID. */
    Optional<UserAccount> findByPersonId(Long personId);

    @Query("SELECT DISTINCT ur.ministryId FROM UserRole ur WHERE ur.role = org.serviceproject.users.entity.Role.SERVICE_SECRETARY AND ur.ministryId IS NOT NULL")
    java.util.Set<Long> findAssignedServiceSecretaryMinistryIds();

    @Query("SELECT DISTINCT ur.classId FROM UserRole ur WHERE ur.role = org.serviceproject.users.entity.Role.CLASS_SECRETARY AND ur.classId IS NOT NULL")
    java.util.Set<Long> findAssignedClassSecretaryClassIds();

    @Query("SELECT DISTINCT u.person.id FROM UserAccount u WHERE u.person.id IN :personIds AND u.enabled = true")
    java.util.Set<Long> findPersonIdsWithEnabledAccount(@Param("personIds") java.util.Collection<Long> personIds);

    @Query("SELECT DISTINCT u FROM UserAccount u " +
           "JOIN FETCH u.person p " +
           "JOIN FETCH u.roles r " +
           "WHERE r.role = org.serviceproject.users.entity.Role.SERVICE_SECRETARY AND p.deletedAt IS NULL AND u.enabled = true")
    java.util.List<UserAccount> findAllServiceSecretaries();

    @Query("SELECT DISTINCT u FROM UserAccount u " +
           "JOIN FETCH u.person p " +
           "JOIN FETCH u.roles r " +
           "WHERE r.role = org.serviceproject.users.entity.Role.CLASS_SECRETARY AND p.deletedAt IS NULL AND u.enabled = true")
    java.util.List<UserAccount> findAllClassSecretaries();

    @Query("SELECT u FROM UserAccount u " +
           "JOIN u.roles r " +
           "JOIN FETCH u.person p " +
           "WHERE r.role = org.serviceproject.users.entity.Role.SERVICE_SECRETARY AND r.ministryId = :ministryId AND p.deletedAt IS NULL")
    java.util.List<UserAccount> findServiceSecretariesByMinistryId(@Param("ministryId") Long ministryId);

    @Query("SELECT u FROM UserAccount u " +
           "JOIN u.roles r " +
           "JOIN FETCH u.person p " +
           "WHERE r.role = org.serviceproject.users.entity.Role.CLASS_SECRETARY AND r.classId = :classId AND p.deletedAt IS NULL")
    java.util.List<UserAccount> findClassSecretariesByClassId(@Param("classId") Long classId);
}

