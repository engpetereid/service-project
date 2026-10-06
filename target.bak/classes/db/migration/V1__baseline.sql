-- ===========================================================================
-- V1: Baseline migration
-- ===========================================================================
-- Verifies Flyway connectivity with the MySQL database.
-- Actual table creation will begin in Phase 2 (V2__create_person_and_user_tables.sql).
--
-- The database must be created manually before running:
--   CREATE DATABASE church_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- ===========================================================================

-- Placeholder: Flyway requires at least one valid SQL statement per migration file.
-- This creates the system_settings table early so that seed data and
-- application configuration can reference it from the start.

CREATE TABLE IF NOT EXISTS system_setting (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    setting_key VARCHAR(100) NOT NULL,
    setting_value VARCHAR(500) NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_system_setting_key UNIQUE (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed the default maximum note score (requirement #30, #80).
INSERT INTO system_setting (setting_key, setting_value, created_at, updated_at)
VALUES ('MAX_NOTE_SCORE', '21', NOW(6), NOW(6));
