package org.serviceproject.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.serviceproject.common.entity.BaseEntity;

/**
 * Associates a {@link Role} with a {@link UserAccount}, optionally scoped
 * to a specific ministry or class.
 * <p>
 * Scope interpretation:
 * <ul>
 *   <li>GENERAL_ADMIN — no scope (global access)</li>
 *   <li>SERVICE_SECRETARY — {@code ministryId} identifies the managed service</li>
 *   <li>CLASS_SECRETARY — {@code classId} identifies the managed class</li>
 *   <li>SERVANT — no scope here (scope comes from StaffPlacement)</li>
 * </ul>
 * A user can have each role at most once (UK on user_id, role).
 */
@Entity
@Table(name = "user_role")
@Getter
@Setter
@NoArgsConstructor
public class UserRole extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    /**
     * Scope: ministry id for SERVICE_SECRETARY.
     * Nullable. FK constraint added in Phase 3 when ministry table exists.
     */
    @Column(name = "ministry_id")
    private Long ministryId;

    /**
     * Scope: class id for CLASS_SECRETARY.
     * Nullable. FK constraint added in Phase 3 when grade_class table exists.
     */
    @Column(name = "class_id")
    private Long classId;

    public UserRole(Role role) {
        this.role = role;
    }

    public UserRole(Role role, Long ministryId, Long classId) {
        this.role = role;
        this.ministryId = ministryId;
        this.classId = classId;
    }
}
