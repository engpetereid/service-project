package org.serviceproject.users.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.serviceproject.users.entity.Role;

import java.util.List;

/**
 * Request DTO for assigning roles to a user.
 */
public record AssignRolesRequest(
        @NotNull(message = "الأدوار مطلوبة")
        List<@Valid RoleAssignment> roles
) {
    /**
     * A single role assignment with optional scope.
     */
    public record RoleAssignment(
            @NotNull(message = "الدور مطلوب")
            Role role,

            /** Required for SERVICE_SECRETARY */
            Long ministryId,

            /** Required for CLASS_SECRETARY */
            Long classId
    ) {}
}
