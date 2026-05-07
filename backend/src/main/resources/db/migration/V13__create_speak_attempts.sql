CREATE TABLE speak_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES learning_sessions(id),
    audio_url VARCHAR(500),
    transcript TEXT,
    ai_feedback TEXT,
    overall_score INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_speak_attempts_session ON speak_attempts(session_id);
