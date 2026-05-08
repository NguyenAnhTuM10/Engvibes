CREATE TABLE user_events (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    UUID         NOT NULL,
    event_type VARCHAR(50)  NOT NULL,
    payload    JSONB        NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_events_user_id      ON user_events(user_id);
CREATE INDEX idx_user_events_created_at   ON user_events(created_at);
