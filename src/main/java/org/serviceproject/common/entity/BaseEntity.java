package org.serviceproject.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Base class for all JPA entities.
 * <p>
 * Provides {@code id}, {@code createdAt}, and {@code updatedAt} with automatic
 * lifecycle callbacks.  Uses {@code @Getter/@Setter} instead of {@code @Data}
 * to avoid issues with {@code equals/hashCode/toString} on JPA proxies and
 * bidirectional relationships (see requirement #108).
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Entity equality is based on database identity only.
     * Two entities are equal if they share the same non-null id.
     * Transient entities (id == null) are never equal to anything except themselves.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return id != null && Objects.equals(id, that.id);
    }

    /**
     * Constant hash code ensures the entity can safely live in hash-based
     * collections across state transitions (transient → managed).
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
