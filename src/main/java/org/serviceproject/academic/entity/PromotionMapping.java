package org.serviceproject.academic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.serviceproject.classes.entity.GradeClass;
import org.serviceproject.common.entity.BaseEntity;

/**
 * Maps a source class to a target class for automatic promotion.
 * <p>
 * Example: "ثانية ابتدائي" → "ثالثة ابتدائي".
 * If {@code isGraduation=true}, the student graduates (no target class).
 * <p>
 * One source class has at most one mapping (UK on source_class_id).
 */
@Entity
@Table(name = "promotion_mapping")
@Getter
@Setter
@NoArgsConstructor
public class PromotionMapping extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_class_id", nullable = false)
    private GradeClass sourceClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_class_id")
    private GradeClass targetClass;

    @Column(name = "is_graduation", nullable = false)
    private boolean graduation = false;
}
