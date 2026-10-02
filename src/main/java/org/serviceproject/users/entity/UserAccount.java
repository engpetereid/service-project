package org.serviceproject.users.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.serviceproject.common.entity.BaseEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Login account for a {@link Person}.
 * <p>
 * One person has at most one account. Username = person's phone number.
 * <p>
 * {@link #tokenVersion} is incremented on password change and phone change
 * to invalidate all previously issued JWT tokens (requirement #5.1).
 */
@Entity
@Table(name = "user_account")
@Getter
@Setter
@NoArgsConstructor
public class UserAccount extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false, unique = true)
    private Person person;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "token_version", nullable = false)
    private int tokenVersion = 0;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserRole> roles = new HashSet<>();

    /**
     * Increment token version to invalidate all existing JWT tokens
     * for this account.
     */
    public void incrementTokenVersion() {
        this.tokenVersion++;
    }

    /**
     * Add a role to this account.
     */
    public void addRole(UserRole role) {
        roles.add(role);
        role.setUser(this);
    }

    /**
     * Check if this account has a specific role.
     */
    public boolean hasRole(Role role) {
        return roles.stream().anyMatch(ur -> ur.getRole() == role);
    }
}
