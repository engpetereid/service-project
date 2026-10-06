package org.serviceproject.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.serviceproject.common.entity.BaseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Shared personal data for both servants and students.
 * <p>
 * Whether a person acts as a servant or student is determined by the existence
 * of {@code StaffPlacement} or {@code StudentPlacement} records respectively.
 * <p>
 * Soft-delete is supported via {@link #deletedAt}.
 */
@Entity
@Table(name = "person")
@Getter
@Setter
@NoArgsConstructor
public class Person extends BaseEntity {

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Column(name = "confession_father")
    private String confessionFather;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
    }
}
