package org.serviceproject.auth.dto;

import java.util.List;

/**
 * Login response containing the JWT token and user information.
 */
public record LoginResponse(
        String token,
        UserInfoResponse user
) {}
