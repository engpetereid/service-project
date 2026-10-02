package org.serviceproject.classes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.serviceproject.common.entity.BaseEntity;
import org.serviceproject.ministries.entity.Ministry;

/**
 * A class within a {@link Ministry} (e.g., "أولى ابتدائي" within "ابتدائي").
 * <p>
 * Classes have a sort order to control their display sequence.
 * Unique constraint: one class name per ministry.
 */
@Entity
@Table(name = "grade_class")
@Getter
@Setter
@NoArgsConstructor
public class GradeClass extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ministry_id", nullable = false)
    private Ministry ministry;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public GradeClass(String name, Ministry ministry) {
        this.name = name;
        this.ministry = ministry;
    }
}
