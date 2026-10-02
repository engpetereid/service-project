package org.serviceproject.ministries.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.serviceproject.common.entity.BaseEntity;

/**
 * A church service / ministry (e.g., ابتدائي, إعدادي).
 * <p>
 * Ministries are managed by GENERAL_ADMIN only.
 * Soft-deactivation via {@link #active} flag instead of deletion
 * to preserve referential integrity in historical data.
 */
@Entity
@Table(name = "ministry")
@Getter
@Setter
@NoArgsConstructor
public class Ministry extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    public Ministry(String name) {
        this.name = name;
    }
}
