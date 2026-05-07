CREATE TABLE shadow_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES learning_sessions(id),
    segment_id UUID NOT NULL REFERENCES subtitle_segments(id),
    attempt_number INT NOT NULL,
    audio_url VARCHAR(500),
    transcript TEXT,
    word_diff TEXT NOT NULL DEFAULT '[]',
    accuracy_score FLOAT NOT NULL DEFAULT 0,
    weak_phonemes_detected TEXT NOT NULL DEFAULT '[]',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_shadow_attempt UNIQUE (session_id, segment_id, attempt_number)
);

CREATE INDEX idx_shadow_attempts_session ON shadow_attempts(session_id);
CREATE INDEX idx_shadow_attempts_segment ON shadow_attempts(segment_id);

CREATE TABLE user_phoneme_stats (
    user_id UUID NOT NULL,
    phoneme VARCHAR(5) NOT NULL,
    total_attempts INT NOT NULL DEFAULT 0,
    errors INT NOT NULL DEFAULT 0,
    last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, phoneme)
);

CREATE INDEX idx_phoneme_stats_user ON user_phoneme_stats(user_id);
