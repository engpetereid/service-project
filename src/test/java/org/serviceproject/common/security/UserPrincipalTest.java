package org.serviceproject.common.security;

import org.junit.jupiter.api.Test;
import org.serviceproject.users.entity.Role;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UserPrincipal}.
 */
class UserPrincipalTest {

    private UserPrincipal createPrincipal(Set<RoleWithScope> roles) {
        return new UserPrincipal(1L, 10L, "01234567890", "hashed", true, 0, roles);
    }

    // ── Role checks ──────────────────────────────────────────────────

    @Test
    void isAdmin_trueWhenHasGeneralAdmin() {
        UserPrincipal principal = createPrincipal(Set.of(
                new RoleWithScope(Role.GENERAL_ADMIN, null, null)
        ));
        assertTrue(principal.isAdmin());
    }

    @Test
    void isAdmin_falseWhenOnlyServant() {
        UserPrincipal principal = createPrincipal(Set.of(
                new RoleWithScope(Role.SERVANT, null, null)
        ));
        assertFalse(principal.isAdmin());
    }

    @Test
    void hasRole_trueForExistingRole() {
        UserPrincipal principal = createPrincipal(Set.of(
                new RoleWithScope(Role.CLASS_SECRETARY, null, 5L),
                new RoleWithScope(Role.SERVANT, null, null)
        ));
        assertTrue(principal.hasRole(Role.CLASS_SECRETARY));
        assertTrue(principal.hasRole(Role.SERVANT));
        assertFalse(principal.hasRole(Role.GENERAL_ADMIN));
    }

    @Test
    void multipleRoles_allDetected() {
        UserPrincipal principal = createPrincipal(Set.of(
                new RoleWithScope(Role.CLASS_SECRETARY, null, 5L),
                new RoleWithScope(Role.SERVANT, null, null)
        ));
        assertTrue(principal.isClassSecretary());
        assertTrue(principal.isServant());
        assertFalse(principal.isAdmin());
        assertFalse(principal.isServiceSecretary());
    }

    // ── Scope extraction ─────────────────────────────────────────────

    @Test
    void getServiceSecretaryMinistryId_returnsIdWhenPresent() {
        UserPrincipal principal = createPrincipal(Set.of(
                new RoleWithScope(Role.SERVICE_SECRETARY, 3L, null)
        ));
        assertEquals(3L, principal.getServiceSecretaryMinistryId());
    }

    @Test
    void getServiceSecretaryMinistryId_returnsNullWhenNotSecretary() {
        UserPrincipal principal = createPrincipal(Set.of(
                new RoleWithScope(Role.SERVANT, null, null)
        ));
        assertNull(principal.getServiceSecretaryMinistryId());
    }

    @Test
    void getClassSecretaryClassId_returnsIdWhenPresent() {
        UserPrincipal principal = createPrincipal(Set.of(
                new RoleWithScope(Role.CLASS_SECRETARY, null, 7L)
        ));
        assertEquals(7L, principal.getClassSecretaryClassId());
    }

    @Test
    void getClassSecretaryClassId_returnsNullWhenNotSecretary() {
        UserPrincipal principal = createPrincipal(Set.of(
                new RoleWithScope(Role.GENERAL_ADMIN, null, null)
        ));
        assertNull(principal.getClassSecretaryClassId());
    }

    // ── UserDetails implementation ───────────────────────────────────

    @Test
    void getUsername_returnsPhone() {
        UserPrincipal principal = createPrincipal(Set.of());
        assertEquals("01234567890", principal.getUsername());
    }

    @Test
    void getAuthorities_containsRolePrefixedAuthorities() {
        UserPrincipal principal = createPrincipal(Set.of(
                new RoleWithScope(Role.GENERAL_ADMIN, null, null),
                new RoleWithScope(Role.SERVANT, null, null)
        ));
        var authorities = principal.getAuthorities();
        assertEquals(2, authorities.size());
        assertTrue(authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GENERAL_ADMIN")));
        assertTrue(authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SERVANT")));
    }

    @Test
    void accountFlags_allTrue() {
        UserPrincipal principal = createPrincipal(Set.of());
        assertTrue(principal.isAccountNonExpired());
        assertTrue(principal.isAccountNonLocked());
        assertTrue(principal.isCredentialsNonExpired());
        assertTrue(principal.isEnabled());
    }

    @Test
    void identity_fieldsCorrect() {
        UserPrincipal principal = createPrincipal(Set.of());
        assertEquals(1L, principal.getUserId());
        assertEquals(10L, principal.getPersonId());
        assertEquals(0, principal.getTokenVersion());
    }
}
