-- Pronunciation sessions: mỗi session gắn với 1 từ/câu cần luyện
CREATE TABLE pronunciation_sessions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id),
    target_text  TEXT NOT NULL,          -- từ/câu cần phát âm, vd: "think"
    target_ipa   TEXT,                   -- IPA tương ứng, vd: "θɪŋk" (lấy từ Python service lần đầu)
    session_type VARCHAR(20) NOT NULL DEFAULT 'WORD',  -- WORD | SENTENCE
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Pronunciation attempts: mỗi lần user ghi âm và submit
CREATE TABLE pronunciation_attempts (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id     UUID NOT NULL REFERENCES pronunciation_sessions(id),
    attempt_number INT NOT NULL DEFAULT 1,
    audio_url      TEXT,
    transcript     TEXT,           -- Whisper nhận ra gì
    actual_ipa     TEXT,           -- IPA của transcript
    overall_score  INT,            -- 0–100
    accuracy_score INT,            -- tỉ lệ phoneme đúng
    fluency_score  INT,            -- tỉ lệ phoneme nói đủ
    phoneme_detail JSONB,          -- [{position, expected, actual, matched, tip}]
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pron_sessions_user ON pronunciation_sessions(user_id);
CREATE INDEX idx_pron_attempts_session ON pronunciation_attempts(session_id);
