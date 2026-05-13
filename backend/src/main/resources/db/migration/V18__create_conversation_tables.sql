CREATE TABLE conversation_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    scenario_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    total_turns INT NOT NULL DEFAULT 0,
    summary TEXT,
    xp_earned INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at TIMESTAMPTZ
);

CREATE TABLE conversation_turns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES conversation_sessions(id) ON DELETE CASCADE,
    turn_number INT NOT NULL,
    user_audio_key VARCHAR(500),
    user_transcript TEXT,
    ai_text TEXT NOT NULL,
    ai_audio_key VARCHAR(500),
    hints_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_conv_sessions_user ON conversation_sessions(user_id);
CREATE INDEX idx_conv_turns_session ON conversation_turns(session_id);
