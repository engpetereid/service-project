package org.serviceproject.users.dto;

import org.serviceproject.auth.dto.RoleInfo;

import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for user data (admin view).
 */
public record UserResponse(
        Long userId,
        Long personId,
        String fullName,
        String phone,
        String gender,
        LocalDate dateOfBirth,
        String address,
        String confessionFather,
        boolean enabled,
        List<RoleInfo> roles
) {}
