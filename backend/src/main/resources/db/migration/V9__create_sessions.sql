CREATE TABLE learning_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    video_id UUID NOT NULL REFERENCES videos(id),
    current_step INT NOT NULL DEFAULT 0,
    completed_steps TEXT NOT NULL DEFAULT '[]',
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    scaffold_level INT,
    total_xp_earned INT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT uq_session_user_video UNIQUE (user_id, video_id)
);

CREATE INDEX idx_sessions_user_id ON learning_sessions(user_id);
CREATE INDEX idx_sessions_video_id ON learning_sessions(video_id);
CREATE INDEX idx_sessions_status ON learning_sessions(status);
