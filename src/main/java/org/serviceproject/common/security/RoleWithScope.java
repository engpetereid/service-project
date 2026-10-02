package org.serviceproject.common.security;

import org.serviceproject.users.entity.Role;

/**
 * A role together with its optional scope (ministry/class).
 * Used inside {@link UserPrincipal} to carry authorization context.
 */
public record RoleWithScope(Role role, Long ministryId, Long classId) {}
