package org.serviceproject.auth.dto;

/**
 * Individual role information with optional scope.
 * Ministry/class names will be populated when those entities exist (Phase 3).
 */
public record RoleInfo(
        String role,
        Long ministryId,
        Long classId
) {}
