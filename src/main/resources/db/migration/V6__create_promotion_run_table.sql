-- ===========================================================================
-- V6: Promotion Run table
-- ===========================================================================

CREATE TABLE promotion_run (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    academic_year_id BIGINT       NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    promoted_count   INT          NOT NULL DEFAULT 0,
    graduated_count  INT          NOT NULL DEFAULT 0,
    skipped_count    INT          NOT NULL DEFAULT 0,
    started_at       DATETIME(6)  NOT NULL,
    completed_at     DATETIME(6),
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_promotion_run_year FOREIGN KEY (academic_year_id) REFERENCES academic_year(id),
    CONSTRAINT uk_promotion_run_year UNIQUE (academic_year_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
