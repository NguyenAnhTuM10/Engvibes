CREATE TABLE flashcard_decks (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id),
    name       VARCHAR(100) NOT NULL,
    color      VARCHAR(7) NOT NULL DEFAULT '#3B82F6',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_decks_user_id ON flashcard_decks(user_id);

CREATE TABLE user_cards (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id),
    vocab_id          UUID NOT NULL REFERENCES vocab_entries(id),
    deck_id           UUID NOT NULL REFERENCES flashcard_decks(id),
    stability         DOUBLE PRECISION NOT NULL DEFAULT 0,
    difficulty        DOUBLE PRECISION NOT NULL DEFAULT 0,
    state             VARCHAR(20) NOT NULL DEFAULT 'NEW',
    last_review       TIMESTAMPTZ,
    next_review       TIMESTAMPTZ,
    review_count      INT NOT NULL DEFAULT 0,
    lapse_count       INT NOT NULL DEFAULT 0,
    context_sentence  TEXT,
    source_video_id   UUID,
    source_segment_id UUID,
    source_type       VARCHAR(20),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, vocab_id, deck_id)
);

CREATE INDEX idx_cards_user_id     ON user_cards(user_id);
CREATE INDEX idx_cards_deck_id     ON user_cards(deck_id);
CREATE INDEX idx_cards_next_review ON user_cards(next_review);
