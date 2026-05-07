CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               VARCHAR(255) UNIQUE NOT NULL,
    username            VARCHAR(50) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    cefr_level          VARCHAR(2) NOT NULL DEFAULT 'A2',
    working_cefr_level  VARCHAR(2) NOT NULL DEFAULT 'A2',
    role                VARCHAR(20) NOT NULL DEFAULT 'USER',
    total_xp            INT NOT NULL DEFAULT 0,
    current_streak_days INT NOT NULL DEFAULT 0,
    last_active_date    DATE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ
);

CREATE INDEX idx_users_email      ON users(email);
CREATE INDEX idx_users_deleted_at ON users(deleted_at);
