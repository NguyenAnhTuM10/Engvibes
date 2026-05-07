CREATE TABLE subtitle_segments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_id    UUID NOT NULL REFERENCES videos(id),
    order_index INT NOT NULL,
    start_ms    INT NOT NULL,
    end_ms      INT NOT NULL,
    text        TEXT NOT NULL,
    word_timings TEXT NOT NULL DEFAULT '[]',
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (video_id, order_index)
);

CREATE INDEX idx_subtitles_video_id ON subtitle_segments(video_id);
CREATE INDEX idx_subtitles_order    ON subtitle_segments(video_id, order_index);
