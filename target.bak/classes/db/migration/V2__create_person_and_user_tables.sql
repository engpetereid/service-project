-- ===========================================================================
-- V2: Person, UserAccount, and UserRole tables
-- ===========================================================================

CREATE TABLE person (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    full_name        VARCHAR(255)  NOT NULL,
    phone            VARCHAR(20)   NOT NULL,
    date_of_birth    DATE,
    address          VARCHAR(500),
    gender           VARCHAR(10)   NOT NULL,
    confession_father VARCHAR(255),
    deleted_at       DATETIME(6),
    created_at       DATETIME(6)   NOT NULL,
    updated_at       DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_person_phone UNIQUE (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_account (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    person_id      BIGINT       NOT NULL,
    password       VARCHAR(255) NOT NULL,
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    token_version  INT          NOT NULL DEFAULT 0,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_account_person UNIQUE (person_id),
    CONSTRAINT fk_user_account_person FOREIGN KEY (person_id) REFERENCES person(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_role (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL,
    role         VARCHAR(30) NOT NULL,
    ministry_id  BIGINT,
    class_id     BIGINT,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT uk_user_role_unique UNIQUE (user_id, role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Note: ministry_id and class_id FK constraints will be added in Phase 3
-- when ministry and grade_class tables are created.

CREATE INDEX idx_person_deleted ON person(deleted_at);
CREATE INDEX idx_person_name ON person(full_name);
CREATE INDEX idx_user_role_user ON user_role(user_id);
