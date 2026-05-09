-- Full-text search index for video discovery
CREATE INDEX IF NOT EXISTS idx_videos_fulltext
    ON videos USING gin(to_tsvector('english', title || ' ' || COALESCE(description, '')));
