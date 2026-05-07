CREATE TABLE vocab_entries (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    word           VARCHAR(100) NOT NULL,
    cefr_level     VARCHAR(2)   NOT NULL,
    part_of_speech VARCHAR(20),
    ipa            VARCHAR(200),
    phonemes       TEXT,
    definition     TEXT,
    frequency_rank INT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_vocab_word_pos ON vocab_entries(word, part_of_speech);
CREATE INDEX idx_vocab_cefr ON vocab_entries(cefr_level);
CREATE INDEX idx_vocab_word ON vocab_entries(word);
