-- ===========================================================================
-- V5: Student Placement table
-- ===========================================================================

CREATE TABLE student_placement (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    person_id          BIGINT       NOT NULL,
    academic_year_id   BIGINT       NOT NULL,
    ministry_id        BIGINT,
    class_id           BIGINT,
    servant_id         BIGINT,
    assigned_at        DATETIME(6),
    status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    guardian_phone     VARCHAR(20),
    talents            TEXT,
    additional_details TEXT,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_student_placement_person FOREIGN KEY (person_id) REFERENCES person(id),
    CONSTRAINT fk_student_placement_year FOREIGN KEY (academic_year_id) REFERENCES academic_year(id),
    CONSTRAINT fk_student_placement_ministry FOREIGN KEY (ministry_id) REFERENCES ministry(id),
    CONSTRAINT fk_student_placement_class FOREIGN KEY (class_id) REFERENCES grade_class(id),
    CONSTRAINT fk_student_placement_servant FOREIGN KEY (servant_id) REFERENCES person(id),
    CONSTRAINT uk_student_placement UNIQUE (person_id, academic_year_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_student_placement_year ON student_placement(academic_year_id);
CREATE INDEX idx_student_placement_ministry ON student_placement(ministry_id);
CREATE INDEX idx_student_placement_class ON student_placement(class_id);
CREATE INDEX idx_student_placement_servant ON student_placement(servant_id);
CREATE INDEX idx_student_placement_status ON student_placement(status);
