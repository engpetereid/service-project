-- ===========================================================================
-- V8: Visit Record table
-- ===========================================================================

CREATE TABLE visit_record (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    student_id       BIGINT       NOT NULL,
    week_id          BIGINT       NOT NULL,
    academic_year_id BIGINT       NOT NULL,
    ministry_snap_id BIGINT       NOT NULL,
    class_snap_id    BIGINT       NOT NULL,
    servant_snap_id  BIGINT,
    method           VARCHAR(10)  NOT NULL,
    prayer_score     INT,
    reading_score    INT,
    note_score       INT,
    notes            TEXT,
    recorded_by      BIGINT       NOT NULL,
    recorded_at      DATETIME(6)  NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_visit_student FOREIGN KEY (student_id) REFERENCES person(id),
    CONSTRAINT fk_visit_week FOREIGN KEY (week_id) REFERENCES week(id),
    CONSTRAINT fk_visit_year FOREIGN KEY (academic_year_id) REFERENCES academic_year(id),
    CONSTRAINT fk_visit_ministry_snap FOREIGN KEY (ministry_snap_id) REFERENCES ministry(id),
    CONSTRAINT fk_visit_class_snap FOREIGN KEY (class_snap_id) REFERENCES grade_class(id),
    CONSTRAINT fk_visit_servant_snap FOREIGN KEY (servant_snap_id) REFERENCES person(id),
    CONSTRAINT fk_visit_recorder FOREIGN KEY (recorded_by) REFERENCES user_account(id),
    CONSTRAINT uk_visit_student_week UNIQUE (student_id, week_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_visit_week ON visit_record(week_id);
CREATE INDEX idx_visit_student ON visit_record(student_id);
CREATE INDEX idx_visit_year ON visit_record(academic_year_id);
CREATE INDEX idx_visit_ministry_snap ON visit_record(ministry_snap_id);
CREATE INDEX idx_visit_class_snap ON visit_record(class_snap_id);
CREATE INDEX idx_visit_servant_snap ON visit_record(servant_snap_id);
