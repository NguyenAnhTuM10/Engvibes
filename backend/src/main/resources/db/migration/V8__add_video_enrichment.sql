ALTER TABLE videos
    ADD COLUMN IF NOT EXISTS warmup_words       TEXT NOT NULL DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS collocations       TEXT NOT NULL DEFAULT '{}',
    ADD COLUMN IF NOT EXISTS summary            TEXT,
    ADD COLUMN IF NOT EXISTS key_points         TEXT NOT NULL DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS speaking_question  TEXT;
