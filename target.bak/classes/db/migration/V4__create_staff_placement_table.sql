-- ===========================================================================
-- V4: Staff Placement table
-- ===========================================================================

CREATE TABLE staff_placement (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    person_id        BIGINT      NOT NULL,
    academic_year_id BIGINT      NOT NULL,
    ministry_id      BIGINT      NOT NULL,
    class_id         BIGINT      NOT NULL,
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_staff_placement_person FOREIGN KEY (person_id) REFERENCES person(id),
    CONSTRAINT fk_staff_placement_year FOREIGN KEY (academic_year_id) REFERENCES academic_year(id),
    CONSTRAINT fk_staff_placement_ministry FOREIGN KEY (ministry_id) REFERENCES ministry(id),
    CONSTRAINT fk_staff_placement_class FOREIGN KEY (class_id) REFERENCES grade_class(id),
    CONSTRAINT uk_staff_placement UNIQUE (person_id, academic_year_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_staff_placement_year ON staff_placement(academic_year_id);
CREATE INDEX idx_staff_placement_ministry ON staff_placement(ministry_id);
CREATE INDEX idx_staff_placement_class ON staff_placement(class_id);
