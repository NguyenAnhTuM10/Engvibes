-- SM-2 Spaced Repetition Vocabulary System
-- Tách hoàn toàn khỏi bảng flashcard/FSRS hiện có (user_cards, flashcard_decks).

CREATE TABLE sm2_decks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id    UUID,           -- null = demo mode (no auth)
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE sm2_cards (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    deck_id          UUID NOT NULL REFERENCES sm2_decks(id) ON DELETE CASCADE,
    front            TEXT NOT NULL,   -- từ tiếng Anh
    back             TEXT NOT NULL,   -- nghĩa tiếng Việt
    ipa              VARCHAR(255),    -- phiên âm IPA (optional)
    example_sentence TEXT,            -- câu ví dụ (optional)
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Trạng thái SRS của 1 card (demo: 1 review state / card, không phân biệt user)
CREATE TABLE sm2_reviews (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id       UUID NOT NULL UNIQUE REFERENCES sm2_cards(id) ON DELETE CASCADE,
    ease_factor   FLOAT   NOT NULL DEFAULT 2.5,  -- EF, khởi đầu 2.5, min 1.3
    interval_days INT     NOT NULL DEFAULT 0,    -- số ngày đến lần ôn tiếp
    repetitions   INT     NOT NULL DEFAULT 0,    -- số lần trả lời đúng liên tiếp
    due_date      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_reviewed TIMESTAMPTZ                    -- null = chưa ôn lần nào
);

CREATE INDEX idx_sm2_cards_deck    ON sm2_cards(deck_id);
CREATE INDEX idx_sm2_reviews_due   ON sm2_reviews(due_date);
CREATE INDEX idx_sm2_reviews_card  ON sm2_reviews(card_id);

-- ──────────────────────────────────────────────────────────────────────────
-- Seed: 1 deck "Daily English" với 15 từ thường gặp
-- Review states: 5 quá hạn, 3 tương lai, 7 chưa review — để test queue dễ
-- ──────────────────────────────────────────────────────────────────────────

INSERT INTO sm2_decks (id, name, description) VALUES
    ('00000000-0000-0000-0000-000000000001',
     'Daily English',
     'Từ vựng tiếng Anh thông dụng — bộ demo');

INSERT INTO sm2_cards (id, deck_id, front, back, ipa, example_sentence) VALUES
    ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
     'achieve',    'đạt được, hoàn thành',
     'əˈtʃiːv',   'She worked hard to achieve her goals.'),

    ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001',
     'significant','đáng kể, quan trọng',
     'sɪɡˈnɪfɪkənt', 'There was a significant improvement in his performance.'),

    ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001',
     'therefore',  'do đó, vì vậy',
     'ˈðɛrfɔː',   'He was ill, therefore he stayed home.'),

    ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001',
     'consequence','hậu quả, kết quả',
     'ˈkɒnsɪkwəns', 'Every action has consequences.'),

    ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000001',
     'perspective','quan điểm, góc nhìn',
     'pəˈspɛktɪv', 'Try to see things from a different perspective.'),

    ('10000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000001',
     'opportunity','cơ hội',
     'ˌɒpəˈtjuːnɪti', 'This is a great opportunity to learn.'),

    ('10000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000001',
     'challenge',  'thách thức',
     'ˈtʃælɪndʒ', 'Learning a new language is a challenge.'),

    ('10000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000001',
     'evidence',   'bằng chứng',
     'ˈɛvɪdəns',  'There is no evidence to support this claim.'),

    ('10000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000001',
     'essential',  'thiết yếu, cần thiết',
     'ɪˈsɛnʃəl',  'Water is essential for life.'),

    ('10000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000001',
     'approach',   'phương pháp, cách tiếp cận',
     'əˈprəʊtʃ',  'We need a new approach to solve this problem.'),

    ('10000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000001',
     'recommend',  'giới thiệu, đề xuất',
     'ˌrɛkəˈmɛnd', 'I highly recommend this book.'),

    ('10000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000001',
     'assume',     'giả định, cho là',
     'əˈsjuːm',   'Never assume you know everything.'),

    ('10000000-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000001',
     'benefit',    'lợi ích',
     'ˈbɛnɪfɪt',  'Exercise has many health benefits.'),

    ('10000000-0000-0000-0000-000000000014', '00000000-0000-0000-0000-000000000001',
     'establish',  'thành lập, xác lập',
     'ɪˈstæblɪʃ', 'The company was established in 1990.'),

    ('10000000-0000-0000-0000-000000000015', '00000000-0000-0000-0000-000000000001',
     'indicate',   'chỉ ra, cho thấy',
     'ˈɪndɪkeɪt', 'The data indicate a clear trend.');

-- Reviews để test queue:
-- 5 quá hạn (due_date trong quá khứ)
INSERT INTO sm2_reviews (card_id, ease_factor, interval_days, repetitions, due_date, last_reviewed) VALUES
    ('10000000-0000-0000-0000-000000000001', 2.5, 1,  1, NOW() - INTERVAL '3 days', NOW() - INTERVAL '4 days'),
    ('10000000-0000-0000-0000-000000000002', 2.3, 6,  2, NOW() - INTERVAL '1 day',  NOW() - INTERVAL '7 days'),
    ('10000000-0000-0000-0000-000000000003', 2.5, 15, 3, NOW() - INTERVAL '5 days', NOW() - INTERVAL '20 days'),
    ('10000000-0000-0000-0000-000000000004', 1.8, 1,  0, NOW() - INTERVAL '2 days', NOW() - INTERVAL '3 days'),
    ('10000000-0000-0000-0000-000000000005', 2.6, 38, 4, NOW() - INTERVAL '1 hour', NOW() - INTERVAL '39 days');

-- 3 tương lai (không xuất hiện trong queue)
INSERT INTO sm2_reviews (card_id, ease_factor, interval_days, repetitions, due_date, last_reviewed) VALUES
    ('10000000-0000-0000-0000-000000000006', 2.5, 6,  1, NOW() + INTERVAL '3 days',  NOW() - INTERVAL '3 days'),
    ('10000000-0000-0000-0000-000000000007', 2.5, 15, 2, NOW() + INTERVAL '10 days', NOW() - INTERVAL '5 days'),
    ('10000000-0000-0000-0000-000000000008', 2.5, 38, 3, NOW() + INTERVAL '30 days', NOW() - INTERVAL '8 days');

-- Cards 9–15: không có review record → new cards, xuất hiện trong queue
