-- ===========================================================================
-- V7: Week table
-- ===========================================================================

CREATE TABLE week (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    start_date DATE         NOT NULL,
    end_date   DATE         NOT NULL,
    deleted_at DATETIME(6),
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_week_start UNIQUE (start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_week_dates ON week(start_date, end_date);
CREATE INDEX idx_week_deleted ON week(deleted_at);
