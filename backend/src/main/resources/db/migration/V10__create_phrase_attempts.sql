CREATE TABLE phrase_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES learning_sessions(id),
    phrase_idx INT NOT NULL,
    audio_url VARCHAR(500),
    transcript TEXT,
    accuracy_score FLOAT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_phrase_attempts_session ON phrase_attempts(session_id);
