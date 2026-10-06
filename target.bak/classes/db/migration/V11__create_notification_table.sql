-- ===========================================================================
-- V11: Notification table
-- ===========================================================================

CREATE TABLE notification (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    user_id       BIGINT        NOT NULL,
    title         VARCHAR(255)  NOT NULL,
    message       TEXT          NOT NULL,
    type          VARCHAR(50)   NOT NULL,
    is_read       BOOLEAN       NOT NULL DEFAULT FALSE,
    read_at       DATETIME(6)   NULL,
    reference_id  VARCHAR(100)  NULL,
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notification_user_id ON notification(user_id);
CREATE INDEX idx_notification_user_read ON notification(user_id, is_read);
CREATE INDEX idx_notification_user_type_ref ON notification(user_id, type, reference_id);
CREATE INDEX idx_notification_created_at ON notification(created_at);
