-- ===========================================================================
-- V12: Servant Weekly Self-Follow-Up table
-- ===========================================================================

CREATE TABLE servant_weekly_follow_up (
    id                          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id                     BIGINT       NOT NULL,
    week_id                     BIGINT       NOT NULL,
    academic_year_id            BIGINT       NOT NULL,
    note_score                  INT,
    max_note_score_snapshot     INT          NOT NULL DEFAULT 21,
    attended_mass               BOOLEAN,
    attended_service_meeting    BOOLEAN,
    attended_tasbeha            BOOLEAN,
    attended_management_meeting BOOLEAN,
    created_at                  DATETIME(6)  NOT NULL,
    updated_at                  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_swfu_user FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE CASCADE,
    CONSTRAINT fk_swfu_week FOREIGN KEY (week_id) REFERENCES week(id) ON DELETE CASCADE,
    CONSTRAINT fk_swfu_year FOREIGN KEY (academic_year_id) REFERENCES academic_year(id) ON DELETE CASCADE,
    CONSTRAINT uk_swfu_user_week UNIQUE (user_id, week_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_swfu_user ON servant_weekly_follow_up(user_id);
CREATE INDEX idx_swfu_week ON servant_weekly_follow_up(week_id);
CREATE INDEX idx_swfu_year ON servant_weekly_follow_up(academic_year_id);
