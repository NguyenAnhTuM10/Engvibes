-- Gộp hệ thống SM-2 (V20: sm2_decks / sm2_cards / sm2_reviews) vào hệ thống FSRS
-- (flashcard_decks / user_cards). Sau migration này chỉ còn MỘT thuật toán ôn tập (FSRS)
-- và MỘT mô hình deck, gắn với user.
--
-- user_cards hỗ trợ 2 loại card:
--   1. Card từ điển  — vocab_id NOT NULL, nội dung lấy từ vocab_entries
--   2. Card tự tạo   — vocab_id NULL, nội dung nằm ở front / back / ipa (import, thêm từ transcript)

-- ── 1. Mở rộng user_cards cho card tự tạo ─────────────────────────────────
ALTER TABLE user_cards ALTER COLUMN vocab_id DROP NOT NULL;
ALTER TABLE user_cards ADD COLUMN front TEXT;
ALTER TABLE user_cards ADD COLUMN back  TEXT;
ALTER TABLE user_cards ADD COLUMN ipa   VARCHAR(255);

ALTER TABLE user_cards ADD CONSTRAINT chk_user_cards_content
    CHECK (vocab_id IS NOT NULL OR (front IS NOT NULL AND back IS NOT NULL));

-- Chống trùng card tự tạo trong cùng deck (không phân biệt hoa thường).
-- Card từ điển đã được chặn trùng bởi UNIQUE (user_id, vocab_id, deck_id) ở V5.
CREATE UNIQUE INDEX uq_user_cards_custom_front
    ON user_cards (deck_id, LOWER(front))
    WHERE vocab_id IS NULL;

-- ── 2. Sửa lỗi card mới không bao giờ đến hạn ─────────────────────────────
-- Trước đây card mới có next_review = NULL nên bị loại khỏi mọi truy vấn
-- "next_review <= now" → không thể ôn. Card mới phải đến hạn ngay khi được tạo.
UPDATE user_cards SET next_review = created_at WHERE next_review IS NULL;
ALTER TABLE user_cards ALTER COLUMN next_review SET DEFAULT NOW();
ALTER TABLE user_cards ALTER COLUMN next_review SET NOT NULL;

-- ── 3. Chuyển dữ liệu SM-2 có chủ sở hữu sang FSRS ────────────────────────
-- Deck SM-2 không có owner_id (chế độ demo không đăng nhập, gồm deck seed
-- "Daily English" của V20) không thể gán cho user nào nên bị bỏ.
-- Trạng thái SM-2 (ease factor / interval) không quy đổi được sang
-- stability / difficulty của FSRS → card được chuyển sang ở trạng thái NEW.
INSERT INTO flashcard_decks (id, user_id, name, created_at)
SELECT d.id, d.owner_id, LEFT(d.name, 100), d.created_at
FROM   sm2_decks d
JOIN   users u ON u.id = d.owner_id;

INSERT INTO user_cards (id, user_id, deck_id, front, back, ipa, context_sentence,
                        state, next_review, source_type, created_at)
SELECT DISTINCT ON (c.deck_id, LOWER(c.front))
       c.id, d.owner_id, c.deck_id, c.front, c.back, c.ipa, c.example_sentence,
       'NEW', NOW(), 'IMPORT', c.created_at
FROM   sm2_cards c
JOIN   sm2_decks d ON d.id = c.deck_id
JOIN   users u     ON u.id = d.owner_id
ORDER  BY c.deck_id, LOWER(c.front), c.created_at;

-- ── 4. Xoá hệ thống SM-2 ──────────────────────────────────────────────────
DROP TABLE sm2_reviews;
DROP TABLE sm2_cards;
DROP TABLE sm2_decks;
