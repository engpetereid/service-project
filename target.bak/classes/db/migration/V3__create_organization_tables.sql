-- ===========================================================================
-- V3: Ministry, GradeClass, AcademicYear, PromotionMapping tables
--     + FK constraints on user_role scope columns
-- ===========================================================================

-- ── Ministry ─────────────────────────────────────────────────────────

CREATE TABLE ministry (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ministry_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── GradeClass ───────────────────────────────────────────────────────

CREATE TABLE grade_class (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    ministry_id BIGINT       NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_grade_class_ministry FOREIGN KEY (ministry_id) REFERENCES ministry(id),
    CONSTRAINT uk_grade_class_ministry_name UNIQUE (ministry_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── AcademicYear ─────────────────────────────────────────────────────

CREATE TABLE academic_year (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(20)  NOT NULL,
    start_date DATE         NOT NULL,
    end_date   DATE         NOT NULL,
    current    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_academic_year_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── PromotionMapping ─────────────────────────────────────────────────

CREATE TABLE promotion_mapping (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    source_class_id  BIGINT      NOT NULL,
    target_class_id  BIGINT,
    is_graduation    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_promo_source_class FOREIGN KEY (source_class_id) REFERENCES grade_class(id),
    CONSTRAINT fk_promo_target_class FOREIGN KEY (target_class_id) REFERENCES grade_class(id),
    CONSTRAINT uk_promo_source UNIQUE (source_class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Add FK constraints on user_role scope columns ────────────────────

ALTER TABLE user_role
    ADD CONSTRAINT fk_user_role_ministry FOREIGN KEY (ministry_id) REFERENCES ministry(id);

ALTER TABLE user_role
    ADD CONSTRAINT fk_user_role_class FOREIGN KEY (class_id) REFERENCES grade_class(id);

-- ── Indexes ──────────────────────────────────────────────────────────

CREATE INDEX idx_grade_class_ministry ON grade_class(ministry_id);
CREATE INDEX idx_academic_year_current ON academic_year(current);
