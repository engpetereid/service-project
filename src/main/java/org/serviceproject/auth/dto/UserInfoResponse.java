package org.serviceproject.auth.dto;

import java.util.List;

/**
 * User information returned from /api/auth/me and inside LoginResponse.
 */
public record UserInfoResponse(
        Long userId,
        Long personId,
        String fullName,
        String phone,
        List<RoleInfo> roles
) {}
