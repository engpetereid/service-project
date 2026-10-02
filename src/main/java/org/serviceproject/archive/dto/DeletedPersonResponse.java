package org.serviceproject.archive.dto;

import org.serviceproject.users.entity.Gender;

import java.time.LocalDateTime;

/**
 * DTO representing a soft-deleted person in the system archive.
 */
public record DeletedPersonResponse(
        Long id,
        String fullName,
        String phone,
        Gender gender,
        String personType,
        LocalDateTime deletedAt,
        String ministryName,
        String className
) {}
