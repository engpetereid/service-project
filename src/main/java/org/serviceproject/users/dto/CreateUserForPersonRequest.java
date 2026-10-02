package org.serviceproject.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request payload for creating a user account for an existing person.
 */
public record CreateUserForPersonRequest(
        @NotBlank(message = "كلمة المرور مطلوبة")
        @Size(min = 6, message = "كلمة المرور يجب أن تكون 6 أحرف على الأقل")
        String password,

        List<AssignRolesRequest.RoleAssignment> roles
) {}

