package org.serviceproject.common.security;

import lombok.Getter;
import org.serviceproject.users.entity.Role;
import org.serviceproject.users.entity.UserAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security principal carrying user identity, roles, and scope.
 * <p>
 * Created from a {@link UserAccount} on every authenticated request
 * by {@link JwtAuthenticationFilter}. Scope is resolved dynamically from
 * the database (requirement #139).
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final Long personId;
    private final String phone;
    private final String password;
    private final boolean enabled;
    private final int tokenVersion;
    private final Set<RoleWithScope> roleScopes;

    public UserPrincipal(Long userId, Long personId, String phone, String password,
                         boolean enabled, int tokenVersion, Set<RoleWithScope> roleScopes) {
        this.userId = userId;
        this.personId = personId;
        this.phone = phone;
        this.password = password;
        this.enabled = enabled;
        this.tokenVersion = tokenVersion;
        this.roleScopes = roleScopes;
    }

    // ── UserDetails implementation ───────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roleScopes.stream()
                .map(rs -> new SimpleGrantedAuthority("ROLE_" + rs.role().name()))
                .collect(Collectors.toSet());
    }

    @Override
    public String getUsername() {
        return phone;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // ── Role helpers ─────────────────────────────────────────────────

    public boolean hasRole(Role role) {
        return roleScopes.stream().anyMatch(rs -> rs.role() == role);
    }

    public boolean isAdmin() {
        return hasRole(Role.GENERAL_ADMIN);
    }

    public boolean isServiceSecretary() {
        return hasRole(Role.SERVICE_SECRETARY);
    }

    public boolean isClassSecretary() {
        return hasRole(Role.CLASS_SECRETARY);
    }

    public boolean isServant() {
        return hasRole(Role.SERVANT);
    }

    /**
     * Returns the ministry ID scoped to the SERVICE_SECRETARY role, or null.
     */
    public Long getServiceSecretaryMinistryId() {
        return roleScopes.stream()
                .filter(rs -> rs.role() == Role.SERVICE_SECRETARY)
                .map(RoleWithScope::ministryId)
                .findFirst()
                .orElse(null);
    }

    public Set<Long> getServiceSecretaryMinistryIds() {
        return roleScopes.stream()
                .filter(rs -> rs.role() == Role.SERVICE_SECRETARY && rs.ministryId() != null)
                .map(RoleWithScope::ministryId)
                .collect(Collectors.toSet());
    }

    /**
     * Returns the class ID scoped to the CLASS_SECRETARY role, or null.
     */
    public Long getClassSecretaryClassId() {
        return roleScopes.stream()
                .filter(rs -> rs.role() == Role.CLASS_SECRETARY)
                .map(RoleWithScope::classId)
                .findFirst()
                .orElse(null);
    }

    public Set<Long> getClassSecretaryClassIds() {
        return roleScopes.stream()
                .filter(rs -> rs.role() == Role.CLASS_SECRETARY && rs.classId() != null)
                .map(RoleWithScope::classId)
                .collect(Collectors.toSet());
    }

    // ── Factory ──────────────────────────────────────────────────────

    /**
     * Build a UserPrincipal from a fully-loaded UserAccount
     * (person and roles must be initialized).
     */
    public static UserPrincipal from(UserAccount account) {
        Set<RoleWithScope> scopes = account.getRoles().stream()
                .map(ur -> new RoleWithScope(ur.getRole(), ur.getMinistryId(), ur.getClassId()))
                .collect(Collectors.toSet());

        return new UserPrincipal(
                account.getId(),
                account.getPerson().getId(),
                account.getPerson().getPhone(),
                account.getPassword(),
                account.isEnabled(),
                account.getTokenVersion(),
                scopes
        );
    }
}
