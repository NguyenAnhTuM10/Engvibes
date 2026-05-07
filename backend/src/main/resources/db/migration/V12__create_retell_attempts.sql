CREATE TABLE retell_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES learning_sessions(id),
    attempt_number INT NOT NULL,
    audio_url VARCHAR(500),
    transcript TEXT,
    scaffold_level INT NOT NULL DEFAULT 1,
    ai_feedback TEXT,
    overall_score INT NOT NULL DEFAULT 0,
    duration_sec INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_retell_attempts_session ON retell_attempts(session_id);
