CREATE TABLE user_video_interaction (
    user_id     UUID         NOT NULL,
    video_id    UUID         NOT NULL REFERENCES videos(id) ON DELETE CASCADE,
    view_count  INT          NOT NULL DEFAULT 0,
    completion_score FLOAT   NOT NULL DEFAULT 0,
    last_viewed TIMESTAMPTZ,
    PRIMARY KEY (user_id, video_id)
);

CREATE INDEX idx_uvi_user_id ON user_video_interaction(user_id);
