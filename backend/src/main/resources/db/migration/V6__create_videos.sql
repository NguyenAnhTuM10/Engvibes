CREATE TABLE videos (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(200)    NOT NULL,
    description     TEXT,
    storage_url     VARCHAR(500)    NOT NULL,
    thumbnail_url   VARCHAR(500),
    duration_sec    INT,
    cefr_level      VARCHAR(2),
    topic           VARCHAR(50),
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    vocab_difficulty    FLOAT DEFAULT 0,
    phonemes_density    JSONB DEFAULT '{}'::jsonb,
    popularity_score    FLOAT DEFAULT 0,
    view_count      INT DEFAULT 0,
    error_message   TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_videos_status      ON videos(status);
CREATE INDEX idx_videos_cefr        ON videos(cefr_level);
CREATE INDEX idx_videos_topic       ON videos(topic);
CREATE INDEX idx_videos_deleted_at  ON videos(deleted_at);
