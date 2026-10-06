-- ===========================================================================
-- V9: Attendance Session, Attendance Record, and Confession Record tables
-- ===========================================================================

CREATE TABLE attendance_session (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    week_id       BIGINT      NOT NULL,
    activity_type VARCHAR(20) NOT NULL,
    session_date  DATE        NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_attendance_session_week FOREIGN KEY (week_id) REFERENCES week(id),
    CONSTRAINT uk_attendance_session UNIQUE (week_id, activity_type, session_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE attendance_record (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    session_id  BIGINT      NOT NULL,
    student_id  BIGINT      NOT NULL,
    present     BOOLEAN     NOT NULL DEFAULT TRUE,
    recorded_by BIGINT      NOT NULL,
    recorded_at DATETIME(6) NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_attendance_record_session FOREIGN KEY (session_id) REFERENCES attendance_session(id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_record_student FOREIGN KEY (student_id) REFERENCES person(id),
    CONSTRAINT fk_attendance_record_recorder FOREIGN KEY (recorded_by) REFERENCES user_account(id),
    CONSTRAINT uk_attendance_record UNIQUE (session_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE confession_record (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    student_id        BIGINT       NOT NULL,
    academic_year_id  BIGINT       NOT NULL,
    confession_date   DATE         NOT NULL,
    confession_father VARCHAR(100),
    notes             TEXT,
    recorded_by       BIGINT       NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_confession_student FOREIGN KEY (student_id) REFERENCES person(id),
    CONSTRAINT fk_confession_year FOREIGN KEY (academic_year_id) REFERENCES academic_year(id),
    CONSTRAINT fk_confession_recorder FOREIGN KEY (recorded_by) REFERENCES user_account(id),
    CONSTRAINT uk_confession_student_date UNIQUE (student_id, confession_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_att_session_week ON attendance_session(week_id);
CREATE INDEX idx_att_record_session ON attendance_record(session_id);
CREATE INDEX idx_att_record_student ON attendance_record(student_id);
CREATE INDEX idx_confession_student ON confession_record(student_id);
CREATE INDEX idx_confession_year ON confession_record(academic_year_id);
