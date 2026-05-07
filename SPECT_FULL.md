# English Learning Platform — Full Technical Spec

> **Project:** Web app học tiếng Anh tập trung Listening & Speaking với AI tích hợp
> **Type:** Đồ án tốt nghiệp ngành CNTT (Kỹ thuật phần mềm)
> **Timeline:** 14-15 tuần (~ 1 semester)
> **Stack:** Spring Boot 3 (Java 21) + React 18 + TypeScript + PostgreSQL + Redis + MinIO + OpenAI

---

## Mục lục
1. [Tổng quan sản phẩm](#1-tổng-quan-sản-phẩm)
2. [Core Learning Loop — 7 bước](#2-core-learning-loop--7-bước)
3. [9 Modules chi tiết](#3-9-modules-chi-tiết)
4. [Database schema](#4-database-schema)
5. [API endpoints](#5-api-endpoints)
6. [AI Layer — kiến trúc và prompts](#6-ai-layer--kiến-trúc-và-prompts)
7. [Recommendation Engine](#7-recommendation-engine)
8. [Frontend architecture](#8-frontend-architecture)
9. [Roadmap 15 tuần](#9-roadmap-15-tuần)
10. [Báo cáo đồ án outline](#10-báo-cáo-đồ-án-outline)

---

## 1. Tổng quan sản phẩm

### 1.1. Vấn đề muốn giải quyết
Người học tiếng Anh tại Việt Nam thường rơi vào tình trạng:
- Học từ vựng nhiều nhưng không nói được
- Xem video tiếng Anh nhưng không hiểu hết, không biết học từ thế nào
- Luyện nói thiếu phản hồi, không biết mình sai chỗ nào
- Apps hiện có (Duolingo, Quizlet) thiếu tính cá nhân hóa, không có "AI coach" thực sự

### 1.2. Giải pháp
Web app học tiếng Anh xoay quanh **video ngắn 30-60s**, mỗi video là 1 vòng học khép kín 7 bước. AI hoạt động như coach cá nhân, giúp user practice nói + cho feedback chi tiết.

### 1.3. Đối tượng
Người học tiếng Anh trình độ **A2 → B2** (sơ-trung cấp), đặc biệt người Việt cần luyện listening và speaking.

### 1.4. USP (Unique Selling Point)
**AI Retelling Coach** — không chỉ chấm phát âm như Elsa, mà đánh giá toàn diện việc kể lại nội dung video bằng ngôn ngữ riêng của user, có model answer mẫu để học theo.

### 1.5. Tech stack chốt

**Backend:**
- Java 21, Spring Boot 3.3
- Spring Security + JWT
- Spring Data JPA + PostgreSQL 16
- Spring WebSocket (cho speaking real-time updates)
- Redis (cache + rate limit)
- MinIO (S3-compatible storage cho video/audio)
- Spring AI hoặc WebClient cho OpenAI integration
- Flyway (DB migrations)
- JUnit 5 + Testcontainers

**Frontend:**
- React 18 + TypeScript + Vite
- TailwindCSS + shadcn/ui
- React Query v5 (data fetching)
- Zustand (complex state)
- React Router v6
- react-player (video)
- recharts (charts cho progress/analytics)

**AI Services:**
- OpenAI Whisper API (ASR)
- OpenAI GPT-4o-mini (LLM tasks)
- CMU Pronouncing Dictionary (phoneme analysis - free)

**Infrastructure:**
- Docker Compose (postgres, redis, minio)
- GitHub Actions (CI cơ bản)

---

## 2. Core Learning Loop — 7 bước

User chọn 1 video trong library, đi qua 7 bước. Tất cả bước **khuyến khích tuần tự nhưng cho skip**.

### Bước 0 — Warmup (Preview vocab)
- 5-8 từ khó nhất video hiện dạng card
- User mark "I know" / "New to me"
- Từ "New to me" auto add vào flashcard deck
- Có thể skip toàn bộ
- Thời gian: 1-2 phút

**Mục đích:** Pre-teach vocab quan trọng để khi nghe user hiểu nội dung tốt hơn.

### Bước 1 — Listen (Xem video + click vocab)
- Video player với phụ đề interactive
- Click từng từ → popup nghĩa + add flashcard
- CEFR highlight tô màu từ theo level
- Có thể xem nhiều lần
- Thời gian: 3-5 phút

**Mục đích:** Hiểu nội dung qua nghe + đọc, mở rộng vocab gặp được.

### Bước 2 — Phrase Practice (Luyện cụm từ)
- 5-10 collocations/cụm từ hay đã extract sẵn
- Mỗi cụm: hiện text + play audio mẫu
- User record nói lại từng cụm
- Whisper check phát âm
- Có thể retry, replay audio mẫu
- Thời gian: 3-5 phút

**Mục đích:** Tập trung phát âm chính xác cụm từ trước khi shadowing cả câu.

### Bước 3 — Shadowing (Nói theo câu)
- Video chia thành các câu (theo subtitle segments)
- Từng câu: play audio gốc → user record nói theo
- Whisper transcribe → so word-by-word với câu gốc
- Highlight từ đúng (xanh) / từ sai/thiếu (đỏ)
- Detect phoneme weakness từ data này
- Retry 3 lần/câu
- Thời gian: 5-10 phút

**Mục đích:** Luyện ngữ điệu, tốc độ, phát âm cả câu giống bản gốc.

### Bước 4 — Retelling ★ (Điểm sáng AI)
- Prompt: "Giờ hãy kể lại nội dung video bằng lời của bạn"
- User chọn 1 trong **4 scaffolding levels**:
  - L1 — No help
  - L2 — Word bank (chips key vocab)
  - L3 — Sentence starters
  - L4 — Story frame (full template)
- User record audio (30-60s)
- Whisper transcribe → LLM evaluate dựa trên **key points** của video
- Feedback chi tiết: coverage score, vocab used/missed, grammar issues, model answer
- Retry không giới hạn
- Thời gian: 5-10 phút

**Mục đích:** Active recall + paraphrasing, kỹ năng quan trọng nhất khi học ngôn ngữ.

### Bước 5 — Speaking mở rộng
- Hệ thống hiện 1 câu hỏi liên quan topic video (pre-gen)
- Ví dụ video về cooking → "Tell me about your favorite dish to cook"
- Panel gợi ý vocab + collocations từ video (reuse)
- User record + Whisper + LLM feedback
- Scoring: Fluency / Grammar / Vocab variety
- Đếm bao nhiêu vocab từ video user dùng được vào câu trả lời tự do
- Model answer
- Thời gian: 5-10 phút

**Mục đích:** Áp dụng vocab vừa học vào tình huống mới, không chỉ recall lại nguyên xi.

### Bước 6 — Quick Review vocab
- Grid 5-10 thẻ vocab user đã add trong session
- Flip nhanh, mark Easy/Hard cho SRS
- Có thể skip
- Liên kết sang module Flashcard nếu muốn ôn đầy đủ
- Thời gian: 1-2 phút

**Mục đích:** Khoảnh khắc kết thúc session, củng cố vocab vừa học.

**Tổng thời gian/session:** 20-35 phút (tùy độ dài video và kỹ năng user).

---

## 3. 9 Modules chi tiết

### Module 1 — Auth & Profile

**Features:**
- Register: email + username + password + chọn CEFR level (A1-C2)
- Login JWT
- Profile page: avatar, stats tổng (số video đã học, từ đã học, streak), settings
- Update CEFR (manual hoặc auto qua adaptive placement)
- Logout, delete account

**Out of scope:** OAuth, email verification, password reset (có thể thêm nếu thời gian)

**APIs:** `POST /api/auth/register`, `/login`, `/logout`, `GET /api/me`, `PATCH /api/me`

---

### Module 2 — Video Library

**Features:**
- Browse grid videos với pagination
- Filter chips: CEFR level / topic / duration
- Search bar (full-text trên title + description)
- Video card: thumbnail, title, duration, CEFR badge, topic tag
- Click → vào trang video detail
- Recommended videos section ở dashboard (từ Module 8)

**Admin features (chỉ admin role):**
- Upload video + chạy pipeline pre-process
- Edit metadata (title, topic, description)
- Mark video published/unpublished

**Out of scope:** User upload, YouTube import

**Pre-processing pipeline (chạy lúc admin upload, KHÔNG chạy real-time cho user):**
```
1. Extract audio (FFmpeg)
2. Transcribe (Whisper) → transcript với word-level timestamps
3. Detect language → reject nếu không phải English
4. Tokenize + lemmatize subtitle
5. Lookup CEFR cho mỗi vocab (Oxford 5000)
6. Extract collocations bằng LLM
7. Identify warmup words (5-8 từ khó nhất + frequency cao)
8. Generate summary + key_points (cho retelling evaluation)
9. Pre-gen speaking question + suggested hints
10. Save tất cả vào DB → status = PUBLISHED
```

**APIs:**
- `GET /api/videos` (với filter, search, paging)
- `GET /api/videos/{id}` (full data: video, subtitles, vocab, summary, warmup)
- `GET /api/videos/recommended` (gọi Module 8)
- Admin: `POST /api/admin/videos`, `POST /api/admin/videos/{id}/process`

---

### Module 3 — Learning Session (orchestration)

**Features:**
- Mỗi user × video = 1 LearningSession
- Session state: current_step (0-6), completed_steps (set)
- User có thể skip bước, quay lại bước trước qua stepper UI
- Auto-save progress sau mỗi bước
- Resume session khi user quay lại video
- Session summary khi hoàn thành (tóm tắt thành tích)

**State machine:**
```
NEW → IN_PROGRESS → COMPLETED
       ↑       ↓
      paused (rời trang) → resumed
```

**APIs:**
- `POST /api/sessions` body `{videoId}` → tạo hoặc resume session
- `GET /api/sessions/{id}` → full state
- `PATCH /api/sessions/{id}/step` body `{step, action: "complete"|"skip"}` → update progress
- `POST /api/sessions/{id}/finish` → đánh dấu COMPLETED
- `GET /api/sessions/history` → list sessions của user

---

### Module 4 — Shadowing (bước 3 trong flow)

**Features:**
- Chia video thành các câu theo SubtitleSegment
- Mỗi câu: play audio + button record + display text
- User record → upload → Whisper transcribe
- Compare word-by-word: highlight đúng/sai
- Retry tối đa 3 lần/câu, lưu best attempt
- Phoneme weakness detection (Cách A heuristic):
  - Map từ user nói sai → CMU phoneme dict → list phoneme có khả năng yếu
  - Lưu vào user_phoneme_stats để Module 7 phân tích
- Có thể slow audio xuống 0.75x

**Scoring per câu:**
- Word accuracy = matched_words / total_words
- Lưu attempt với accuracy

**Out of scope:** Pronunciation scoring chuyên sâu (GOP per phoneme từ Azure)

**APIs:**
- `GET /api/sessions/{id}/shadow/segments` → list câu
- `POST /api/sessions/{id}/shadow/{segmentIdx}/attempt` multipart audio → trả ScoreResult
- `GET /api/sessions/{id}/shadow/result` → tổng kết bước

---

### Module 5 — AI Retelling Coach ★ (bước 4 - showcase chính)

**Features:**

**5.1. Scaffolding selection:**
4 levels, user chọn dựa vào tự tin:
- **L1 — No help:** Empty screen, chỉ nút mic
- **L2 — Word bank:** Chips key vocab có thể click nghe nghĩa
- **L3 — Sentence starters:** "The video is about...", "First, ...", "In the end..."
- **L4 — Story frame:** Full template với blank cho user nói điền

**5.2. Recording:**
- MediaRecorder UI với waveform visualization
- Max 90 giây
- Replay audio trước khi submit

**5.3. AI Evaluation pipeline:**
```
audio.webm → Whisper API → transcript
                              ↓
        + key_points của video (đã pre-gen)
        + vocab list của video
        + scaffold_level user chọn
                              ↓
                        GPT-4o-mini
                              ↓
                  Structured JSON feedback
```

**5.4. Feedback panel structure:**
```typescript
{
  coverage_score: 0-100,           // % key points đã được kể
  covered_points: [{point, mention_quote}],
  missed_points: [string],
  
  vocab_score: 0-100,
  vocab_used: [{word, in_sentence}],
  vocab_missed: [string],          // chip có nút + add flashcard
  
  grammar_score: 0-100,
  grammar_issues: [{
    error_quote: string,
    correction: string,
    brief_explain: string
  }],                              // max 3 issues
  
  positive_notes: [string],
  improvement_tips: [string],
  
  model_answer: string,            // câu mẫu B2 (collapsible)
  overall_score: 0-100
}
```

**5.5. Retry & comparison:**
- User retry không giới hạn
- Lưu lịch sử attempts để xem tiến bộ qua nhiều lần
- Show "best attempt so far"

**APIs:**
- `POST /api/sessions/{id}/retell/start` body `{scaffold_level: 1-4}` → trả scaffold content (vocab/starters/frame tùy level)
- `POST /api/sessions/{id}/retell/attempt` multipart audio → trả full feedback
- `GET /api/sessions/{id}/retell/attempts` → list lịch sử attempts

---

### Module 6 — Speaking Practice (bước 5)

**Features:**
- Pre-generated question từ video (đã pre-gen lúc admin upload)
- Hints panel: vocab + collocations từ video (reuse data)
- User record + submit
- Whisper + LLM eval
- Scoring khác Retelling: focus vào fluency + vocab variety + apply video vocab
- Model answer

**Khác Retelling thế nào:**
- Retelling: kể lại CHÍNH XÁC nội dung video
- Speaking: trả lời câu hỏi LIÊN QUAN, focus dùng vocab vừa học

**APIs:**
- `GET /api/sessions/{id}/speak/question` → trả câu hỏi + hints
- `POST /api/sessions/{id}/speak/attempt` multipart audio → trả feedback

---

### Module 7 — Vocab & Flashcard

**Features:**

**7.1. Vocab management:**
- VocabEntry pre-seeded từ Oxford 5000 (CEFR đã tag)
- Mỗi từ có: word, lemma, IPA, pos, definition (EN+VI), examples, CEFR
- Search vocab
- Manual add từ mới (admin) hoặc auto extract từ video

**7.2. Deck management:**
- User tạo nhiều deck (default 1 deck "My vocabulary")
- Đặt tên, màu cho deck
- Move card giữa decks

**7.3. Add vocab to deck (4 nguồn):**
- Từ Warmup (auto add những từ "New to me")
- Từ Listen (click từ trong subtitle)
- Từ Retell feedback (nút + trên missed vocab chips)
- Từ Speak feedback (tương tự)

**Mỗi card lưu context:** câu video gốc, video_id, segment_id, timestamp

**7.4. SRS Algorithm — FSRS (Free Spaced Repetition Scheduler):**
- Modern algorithm thay SM-2 cũ
- Params: stability, difficulty, retrievability
- Review rating: Again (1) / Hard (2) / Good (3) / Easy (4)
- Tự động tính next_review timestamp
- Reference: open-spaced-repetition/py-fsrs hoặc port Java/Kotlin

**7.5. Review modes:**
- Classic flip (Space để lật)
- Type the answer
- Multiple choice (4 options)
- Listen & type (audio mode)

**7.6. Quick review (bước 6 trong flow):**
- 5-10 thẻ vừa add session hiện tại
- Light UX, không bắt buộc
- Liên kết sang full Flashcard module

**APIs:**
- `GET /api/vocab/search?q=...`
- `GET /api/vocab/{id}`
- `POST /api/decks`, `GET /api/decks`, `PATCH /api/decks/{id}`
- `POST /api/cards` body `{deckId, vocabId, contextSentence?, sourceVideoId?, sourceSegmentId?}`
- `GET /api/decks/{id}/cards/due`
- `POST /api/cards/{id}/review` body `{rating: 1-4}`

---

### Module 8 — Recommendation Engine ★ (showcase AI ứng dụng)

**Sub-features:**

**8.1. Next video recommendation (content-based — primary):**

User feature vector:
```typescript
{
  cefr_level: "B1",
  weak_phonemes: ["θ", "ð", "r", "v"],     // top 5
  recent_topics: ["food", "travel"],         // last 10 sessions
  vocab_known_count_by_cefr: {A1: 800, A2: 600, B1: 200, B2: 50},
  active_session_count: 42                   // total completed sessions
}
```

Video feature vector (pre-computed lúc admin upload):
```typescript
{
  cefr_level: "B1",
  topic: "food",
  duration_sec: 45,
  phonemes_density: {θ: 0.05, r: 0.12, ...},  // % câu chứa phoneme
  vocab_difficulty: 0.35,                       // % vocab > B1
  popularity_score: 0.78                        // tính từ user views
}
```

**Score calculation:**
```python
score(user, video) = (
    0.30 * cefr_match(user.cefr, video.cefr) +      # gần level user nhất
    0.25 * topic_diversity(user.recent_topics, video.topic) +  # đa dạng
    0.25 * weakness_match(user.weak_phonemes, video.phonemes_density) +  # giúp luyện điểm yếu
    0.10 * popularity +
    0.10 * not_seen_yet
)
```

Top 5-10 video score cao nhất → recommend.

**8.2. Collaborative filtering (PoC với synthetic data):**
- Sinh 50-100 fake users với pattern khác nhau (script seed)
- Build user × video rating matrix (rating = average score user khi học video đó)
- SVD decomposition (Apache Commons Math hoặc lib khác)
- Predict rating cho user × unwatched_video
- Reference paper Netflix Prize trong báo cáo

**Ghi rõ trong báo cáo:** đây là PoC vì đồ án chưa có user thật đủ data.

**8.3. Vocab review prioritization (FSRS + weakness):**
- Trong số các từ FSRS đến hạn review, ưu tiên từ:
  - Chứa phoneme user yếu
  - Có CEFR level đang là target (B1 cho user B1)
  - Source từ video gần đây (reinforcement)

**8.4. Daily challenge widget (dashboard):**
- Mỗi ngày suggest:
  - 1 video mới (từ 8.1)
  - X từ cần ôn (từ 8.3)
  - 1 mini phrase practice (random từ video user đã xem)
- Hoàn thành đủ → bonus XP

**8.5. Adaptive placement:**
- Dùng warmup feedback (% "new to me") làm signal:
  - Liên tục < 20% new → có thể up level (suggest video B2)
  - Liên tục > 60% new → suggest video dễ hơn
- Update working_cefr_level tự động sau mỗi 10 sessions

**APIs:**
- `GET /api/recommend/videos` → top recommended videos
- `GET /api/recommend/vocab-priority` → next vocab to review
- `GET /api/recommend/daily-challenge`

---

### Module 9 — Progress & Analytics

**Features:**

**9.1. Stats overview (dashboard widget):**
- Streak days (số ngày liên tục)
- Total XP (tích lũy từ activities)
- Videos completed (full 7 steps)
- Vocab mastered (cards với stability > 30 ngày)
- Average retell score 7 ngày qua

**9.2. Weekly activity chart:**
- Recharts BarChart
- X axis: 7 ngày qua
- Y axis: phút active learning / ngày
- Stack bar phân theo activity (listen / shadow / retell / speak)

**9.3. Phoneme weakness heatmap:**
- Hiển thị 24 phoneme của English
- Màu intensity = số lần sai / tổng lần dùng
- Click phoneme → list video chứa phoneme đó (gợi ý luyện)

**9.4. Vocab growth chart:**
- Line chart vocab learned theo thời gian
- Phân tách theo CEFR level (A1, A2, B1, B2 lines khác nhau)

**9.5. Session history:**
- Table list sessions với filter (date range, video topic, completion status)
- Click 1 session → xem chi tiết: scores, audio recordings, retell attempts

**9.6. Behavior tracking:**
- Log 5 events:
  - `replay_audio` (replay câu trong shadow)
  - `skip_step` (skip bước nào trong flow)
  - `retry_retell` (retry retelling)
  - `time_on_word` (thời gian dwell trên popup vocab)
  - `add_vocab` (add từ mới với context source)
- Lưu vào bảng `user_events`
- Phục vụ:
  - Heuristic improvements
  - Recommendation system input
  - Báo cáo phân tích behavior pattern

**APIs:**
- `GET /api/stats/overview`
- `GET /api/stats/weekly`
- `GET /api/stats/phonemes`
- `GET /api/stats/vocab-growth`
- `GET /api/sessions/history`
- `POST /api/events` (batch endpoint cho FE log)

---

## 4. Database schema

### Core tables

```sql
users (
  id UUID PK,
  email VARCHAR UNIQUE,
  username VARCHAR,
  password_hash VARCHAR,
  cefr_level VARCHAR(2),         -- 'A1','A2','B1','B2','C1','C2'
  working_cefr_level VARCHAR(2), -- adaptive, có thể khác cefr_level
  role VARCHAR DEFAULT 'USER',   -- 'USER', 'ADMIN'
  total_xp INT DEFAULT 0,
  current_streak_days INT DEFAULT 0,
  last_active_date DATE,
  created_at TIMESTAMPTZ
)

vocab_entries (
  id UUID PK,
  word VARCHAR UNIQUE,
  lemma VARCHAR,
  pos VARCHAR(10),               -- NOUN/VERB/ADJ/ADV/OTHER
  cefr_level VARCHAR(2),
  ipa VARCHAR,
  definition_en TEXT,
  definition_vi TEXT,
  examples JSONB,                -- [string]
  phonemes JSONB                 -- ["DH", "IH", "S"] - CMU notation
)

collocations (
  id UUID PK,
  head_vocab_id UUID FK,
  phrase VARCHAR,
  pattern VARCHAR,               -- "verb + noun"
  example TEXT
)
```

### Video tables

```sql
videos (
  id UUID PK,
  title VARCHAR,
  description TEXT,
  storage_url VARCHAR,           -- MinIO key
  thumbnail_url VARCHAR,
  duration_sec INT,
  cefr_level VARCHAR(2),
  topic VARCHAR,                 -- food/travel/tech/...
  status VARCHAR,                -- DRAFT/PROCESSING/PUBLISHED
  vocab_difficulty FLOAT,        -- % vocab > target CEFR
  phonemes_density JSONB,        -- {"DH": 0.05, "R": 0.12, ...}
  popularity_score FLOAT DEFAULT 0,
  view_count INT DEFAULT 0,
  created_at TIMESTAMPTZ
)

subtitle_segments (
  id UUID PK,
  video_id UUID FK,
  order_index INT,
  start_ms INT,
  end_ms INT,
  text TEXT,
  word_timings JSONB             -- [{word, start_ms, end_ms, phonemes}]
)

video_vocab (
  id UUID PK,
  video_id UUID FK,
  vocab_id UUID FK,
  segment_id UUID FK,
  word_in_context VARCHAR,
  start_ms INT
)

video_collocations (
  id UUID PK,
  video_id UUID FK,
  phrase VARCHAR,
  pattern VARCHAR,
  example_from_video TEXT,
  segment_id UUID FK
)

video_warmup_words (
  id UUID PK,
  video_id UUID FK,
  vocab_id UUID FK,
  priority_order INT             -- 1-8
)

video_summary (
  id UUID PK,
  video_id UUID FK UNIQUE,
  short_summary TEXT,            -- 3 câu cho user đọc
  key_points JSONB,              -- ["main idea", "supporting point 1", ...]
  storytelling_recap TEXT
)

video_speaking_questions (
  id UUID PK,
  video_id UUID FK,
  question TEXT,
  suggested_vocab JSONB,         -- ["word1", "word2"]
  suggested_collocations JSONB,
  sample_opening TEXT,
  structure_tips JSONB
)
```

### Learning Session tables

```sql
learning_sessions (
  id UUID PK,
  user_id UUID FK,
  video_id UUID FK,
  current_step INT,              -- 0-6
  completed_steps JSONB,         -- [0, 1, 2]
  status VARCHAR,                -- IN_PROGRESS/COMPLETED
  scaffold_level INT,            -- chosen for retell (1-4)
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  total_xp_earned INT,
  UNIQUE(user_id, video_id)
)

shadow_attempts (
  id UUID PK,
  session_id UUID FK,
  segment_id UUID FK,
  attempt_number INT,            -- 1, 2, 3
  audio_url VARCHAR,
  transcript TEXT,
  word_diff JSONB,               -- [{expected, actual, status}]
  accuracy_score FLOAT,          -- 0-1
  weak_phonemes_detected JSONB,  -- ["TH", "R"]
  created_at TIMESTAMPTZ
)

retell_attempts (
  id UUID PK,
  session_id UUID FK,
  attempt_number INT,
  audio_url VARCHAR,
  transcript TEXT,
  scaffold_level INT,
  ai_feedback JSONB,             -- full feedback structure
  overall_score INT,
  duration_sec INT,
  created_at TIMESTAMPTZ
)

speak_attempts (
  id UUID PK,
  session_id UUID FK,
  question_id UUID FK,
  audio_url VARCHAR,
  transcript TEXT,
  ai_feedback JSONB,
  overall_score INT,
  created_at TIMESTAMPTZ
)
```

### Flashcard tables

```sql
flashcard_decks (
  id UUID PK,
  user_id UUID FK,
  name VARCHAR,
  color VARCHAR(7),              -- '#3B8BD4'
  is_default BOOLEAN,
  created_at TIMESTAMPTZ
)

user_cards (
  id UUID PK,
  user_id UUID FK,
  vocab_id UUID FK,
  deck_id UUID FK,
  
  -- FSRS fields
  stability FLOAT,
  difficulty FLOAT,
  state VARCHAR,                 -- NEW/LEARNING/REVIEW/RELEARNING
  last_review TIMESTAMPTZ,
  next_review TIMESTAMPTZ,
  review_count INT DEFAULT 0,
  lapse_count INT DEFAULT 0,
  
  -- Context
  context_sentence TEXT,
  source_video_id UUID FK,
  source_segment_id UUID FK,
  source_type VARCHAR,           -- WARMUP/LISTEN/RETELL/SPEAK/MANUAL
  
  created_at TIMESTAMPTZ,
  UNIQUE(user_id, vocab_id, deck_id)
)
```

### Analytics tables

```sql
user_events (
  id BIGSERIAL PK,
  user_id UUID,
  event_type VARCHAR,            -- replay_audio/skip_step/retry_retell/...
  payload JSONB,
  created_at TIMESTAMPTZ,
  INDEX(user_id, created_at)
)

user_phoneme_stats (
  user_id UUID FK,
  phoneme VARCHAR(5),            -- "TH", "R", "V"
  total_attempts INT,
  errors INT,
  last_updated TIMESTAMPTZ,
  PRIMARY KEY(user_id, phoneme)
)

user_video_interaction (
  user_id UUID FK,
  video_id UUID FK,
  view_count INT,
  completion_score FLOAT,        -- 0-1, dùng cho recommendation
  last_viewed TIMESTAMPTZ,
  PRIMARY KEY(user_id, video_id)
)
```

---

## 5. API endpoints (tóm gọn)

### Auth
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/me`
- `PATCH /api/me`

### Videos
- `GET /api/videos` (filter, paging, search)
- `GET /api/videos/{id}`
- `GET /api/videos/recommended`

### Admin (role ADMIN)
- `POST /api/admin/videos` (multipart upload)
- `POST /api/admin/videos/{id}/process`
- `GET /api/admin/videos/{id}/processing-status`
- `PATCH /api/admin/videos/{id}` (edit metadata)

### Sessions (orchestration)
- `POST /api/sessions` `{videoId}`
- `GET /api/sessions/{id}`
- `PATCH /api/sessions/{id}/step` `{step, action}`
- `POST /api/sessions/{id}/finish`
- `GET /api/sessions/history`

### Warmup (bước 0)
- `GET /api/sessions/{id}/warmup` → trả 5-8 vocab
- `POST /api/sessions/{id}/warmup/mark` `{vocabId, status: "known"|"new"}`

### Listen (bước 1)
- `POST /api/sessions/{id}/listen/add-vocab` `{vocabId, segmentId}` → quick add

### Phrase Practice (bước 2)
- `GET /api/sessions/{id}/phrases` → list collocations
- `POST /api/sessions/{id}/phrases/{idx}/attempt` multipart audio

### Shadow (bước 3)
- `GET /api/sessions/{id}/shadow/segments`
- `POST /api/sessions/{id}/shadow/{segmentIdx}/attempt` multipart
- `GET /api/sessions/{id}/shadow/result`

### Retell (bước 4) ★
- `POST /api/sessions/{id}/retell/start` `{scaffold_level}`
- `POST /api/sessions/{id}/retell/attempt` multipart
- `GET /api/sessions/{id}/retell/attempts`

### Speak (bước 5)
- `GET /api/sessions/{id}/speak/question`
- `POST /api/sessions/{id}/speak/attempt` multipart

### Vocab & Flashcard
- `GET /api/vocab/search?q=...`
- `GET /api/vocab/{id}`
- `POST /api/decks` / `GET /api/decks` / `PATCH /api/decks/{id}` / `DELETE /api/decks/{id}`
- `POST /api/cards` (add card)
- `GET /api/decks/{id}/cards`
- `GET /api/decks/{id}/cards/due`
- `POST /api/cards/{id}/review` `{rating}`
- `GET /api/sessions/{id}/quick-review` (bước 6)

### Recommendation
- `GET /api/recommend/videos`
- `GET /api/recommend/vocab-priority`
- `GET /api/recommend/daily-challenge`

### Stats & Analytics
- `GET /api/stats/overview`
- `GET /api/stats/weekly`
- `GET /api/stats/phonemes`
- `GET /api/stats/vocab-growth`
- `POST /api/events` (batch FE → BE log)

---

## 6. AI Layer — kiến trúc và prompts

### 6.1. AIOrchestrationService — central layer

Mọi LLM call đi qua service này. Lý do:
- Centralize prompt templates → dễ A/B test
- Centralize caching strategy
- Centralize error handling + retry
- Track cost per user

```java
@Service
class AIOrchestrationService {
    private final WhisperClient whisper;
    private final LLMClient llm;          // OpenAI WebClient
    private final PromptRegistry prompts;
    private final RedisTemplate cache;
    
    // High-level methods
    WhisperResult transcribe(InputStream audio, String filename);
    
    VideoSummary summarizeVideo(String transcript, String userLevel);
    List<KeyPoint> extractKeyPoints(String transcript);
    List<String> identifyWarmupWords(String transcript);
    SpeakingQuestionSet generateSpeakingQuestion(String transcript, String topic);
    
    RetellFeedback evaluateRetell(RetellEvalRequest req);   // ★ điểm sáng
    SpeakFeedback evaluateSpeak(SpeakEvalRequest req);
    
    List<Collocation> extractCollocations(String text);
}
```

### 6.2. Prompt templates — lưu file

Folder `src/main/resources/prompts/`:
- `video_summary.txt`
- `key_points_extract.txt`
- `warmup_words.txt`
- `speaking_question_gen.txt`
- `retell_evaluate.txt` ★
- `speak_evaluate.txt`
- `collocation_extract.txt`

### 6.3. Retell evaluate prompt (điểm sáng — viết kỹ)

File `retell_evaluate.txt`:

```
You are a supportive English speaking coach evaluating how well a {user_level} 
learner retold a video they just watched.

VIDEO CONTEXT:
- Short summary: {short_summary}
- Key points (these are the ground truth the user should cover):
{key_points_numbered}
- Important vocabulary from video: {key_vocab_list}
- Important collocations: {collocations_list}

USER'S RETELLING:
- Transcript: "{user_transcript}"
- Duration: {duration_sec}s
- Scaffold level used: {scaffold_level} (1=no help, 4=full template)

EVALUATION TASK:
Return a JSON object (no markdown, no code fences) with this exact structure:

{
  "coverage_score": <int 0-100>,
  "covered_points": [
    {
      "point": "<key point text>",
      "user_mention_quote": "<exact phrase from user transcript>"
    }
  ],
  "missed_points": ["<key point text>"],
  
  "vocab_score": <int 0-100>,
  "vocab_used": [
    {"word": "<word>", "in_sentence": "<user's sentence containing it>"}
  ],
  "vocab_missed": ["<word from key vocab not used>"],
  
  "grammar_score": <int 0-100>,
  "grammar_issues": [
    {
      "error_quote": "<exact wrong text from user>",
      "correction": "<corrected version>",
      "brief_explain": "<1 sentence why>"
    }
  ],
  
  "positive_notes": ["<specific good thing the user did>"],
  "improvement_tips": ["<focused actionable tip>"],
  
  "model_answer": "<a natural {user_level} answer that covers all key points 
                    using suggested vocabulary in 2-3 sentences>",
  
  "overall_score": <int 0-100>
}

RULES:
- Be encouraging. Lead with what user did well.
- Maximum 3 grammar issues (most impactful only).
- If scaffold_level >= 3, don't penalize for using the scaffold.
- Coverage score: how many key points are clearly addressed (paraphrase OK).
- Vocab score: % of suggested vocab actually used (or attempted).
- Grammar score: subjective but consider clarity over perfection.
- Overall = 0.5*coverage + 0.25*vocab + 0.25*grammar.
- Model answer should naturally use 60-80% of suggested vocab.
```

### 6.4. Caching strategy

| AI task | Cache? | TTL | Key |
|---|---|---|---|
| Video summary | YES | forever | `ai:summary:{video_id}` |
| Key points extraction | YES | forever | `ai:keypoints:{video_id}` |
| Warmup words | YES | forever | `ai:warmup:{video_id}` |
| Speaking question gen | YES | forever | `ai:question:{video_id}` |
| Collocation extract | YES | forever | `ai:colloc:{video_id}` |
| Retell evaluate | NO | - | personalized |
| Speak evaluate | NO | - | personalized |
| Whisper transcribe | NO | - | unique audio |

→ Mỗi video chỉ pre-process **1 lần duy nhất**. User xem free.

### 6.5. Rate limiting

- Per user: 30 AI requests / phút
- Per user: 50 retell + speak attempts / ngày
- Per user: 30 phrase practice attempts / ngày

Nếu vượt: HTTP 429 với `Retry-After` header. UI hiện thông báo thân thiện.

---

## 7. Recommendation Engine

### 7.1. Content-based filtering (primary)

Implementation chính cho production. Khả thi với 1 user.

**Algorithm:**
1. Build `UserFeatureVector` cho user hiện tại
2. Build/load `VideoFeatureVector` cho mỗi video chưa xem
3. Compute weighted score
4. Return top 10

**Code structure:**
```java
@Service
class ContentBasedRecommender {
    public List<Video> recommend(UserId userId, int limit) {
        UserFeatureVector user = buildUserVector(userId);
        List<Video> candidates = videoRepo.findUnseenByUser(userId);
        
        return candidates.stream()
            .map(v -> new ScoredVideo(v, computeScore(user, v.getFeatures())))
            .sorted(Comparator.comparingDouble(ScoredVideo::score).reversed())
            .limit(limit)
            .map(ScoredVideo::video)
            .toList();
    }
    
    private double computeScore(UserFeatureVector u, VideoFeatureVector v) {
        return 0.30 * cefrMatch(u, v)
             + 0.25 * topicDiversity(u, v)
             + 0.25 * weaknessMatch(u, v)
             + 0.10 * v.popularity()
             + 0.10 * recencyBoost(v);
    }
}
```

### 7.2. Collaborative filtering — SVD (PoC)

Demo để show kiến thức Matrix Factorization. Dùng synthetic data.

**Setup:**
- Script seed 50-100 fake users với pattern khác nhau
- Mỗi fake user có `user_video_interaction` records với completion_score
- Build matrix M[user × video] = completion_score
- SVD: `M ≈ U × Σ × V^T`
- Predict `score(user_i, video_j) = U[i] · Σ · V[j]^T`

**Lib:** Apache Commons Math `SingularValueDecomposition`

**Trong báo cáo:** giải thích thuật toán, công thức, complexity, so sánh với content-based, lý do PoC.

### 7.3. Hybrid (combine 2 methods)

Khi đã có cả 2:
```
final_score = 0.7 * content_based + 0.3 * collaborative
```

Khi user mới (chưa có data) → 100% content-based.
Khi user có > 30 sessions → tăng dần weight collaborative.

### 7.4. Weakness-based vocab review

Trong số `due_cards` từ FSRS, sort lại priority:

```python
def vocab_priority(card, user):
    score = 1.0
    
    # phoneme weakness boost
    weak_phoneme_count = len(set(card.vocab.phonemes) & set(user.weak_phonemes))
    score += 0.3 * weak_phoneme_count
    
    # CEFR matching
    if card.vocab.cefr == user.working_cefr_level:
        score += 0.2
    
    # recent video reinforcement
    if card.source_video_viewed_recently:
        score += 0.1
    
    # urgency (overdue)
    days_overdue = (now - card.next_review).days
    score += 0.1 * max(0, days_overdue)
    
    return score
```

### 7.5. Daily challenge composition

```python
def build_daily_challenge(user):
    return {
        "video": content_based_recommender.recommend(user, limit=1)[0],
        "vocab": prioritize_vocab(user, limit=5),
        "phrase_practice": pick_random_phrase_from_user_history(user)
    }
```

---

## 8. Frontend architecture

### 8.1. Folder structure

```
frontend/src/
├── app/
│   ├── App.tsx
│   ├── providers.tsx           # QueryClient, Theme, etc.
│   └── router.tsx
├── pages/
│   ├── HomePage.tsx            # dashboard
│   ├── VideosPage.tsx          # library
│   ├── VideoSessionPage.tsx    # 7-step session host
│   ├── DecksPage.tsx
│   ├── DeckDetailPage.tsx
│   ├── ReviewPage.tsx          # full flashcard review
│   ├── ProgressPage.tsx        # analytics
│   ├── HistoryPage.tsx
│   └── ProfilePage.tsx
├── features/
│   ├── auth/
│   ├── videos/
│   │   ├── api.ts
│   │   ├── components/
│   │   │   ├── VideoCard.tsx
│   │   │   ├── VideoFilters.tsx
│   │   │   └── RecommendedSection.tsx
│   │   └── types.ts
│   ├── session/
│   │   ├── api.ts
│   │   ├── store.ts            # Zustand for active session state
│   │   ├── components/
│   │   │   ├── StepNavigator.tsx
│   │   │   ├── steps/
│   │   │   │   ├── WarmupStep.tsx
│   │   │   │   ├── ListenStep.tsx
│   │   │   │   ├── PhraseStep.tsx
│   │   │   │   ├── ShadowStep.tsx
│   │   │   │   ├── RetellStep.tsx     # ★ component lớn nhất
│   │   │   │   ├── SpeakStep.tsx
│   │   │   │   └── QuickReviewStep.tsx
│   │   │   ├── VideoPlayer.tsx
│   │   │   ├── SubtitleOverlay.tsx
│   │   │   ├── ClickableWord.tsx
│   │   │   └── VocabPopup.tsx
│   ├── retell/                 # tách riêng vì phức tạp
│   │   ├── components/
│   │   │   ├── ScaffoldSelector.tsx
│   │   │   ├── ScaffoldL1Empty.tsx
│   │   │   ├── ScaffoldL2WordBank.tsx
│   │   │   ├── ScaffoldL3Starters.tsx
│   │   │   ├── ScaffoldL4Frame.tsx
│   │   │   ├── AudioRecorder.tsx
│   │   │   ├── FeedbackPanel.tsx
│   │   │   └── AttemptHistory.tsx
│   ├── flashcard/
│   ├── stats/
│   │   └── components/
│   │       ├── StreakCard.tsx
│   │       ├── WeeklyChart.tsx
│   │       ├── PhonemeHeatmap.tsx
│   │       └── VocabGrowthChart.tsx
│   └── recommend/
└── shared/
    ├── ui/                     # shadcn components
    ├── api/
    │   ├── client.ts           # axios setup
    │   └── ws.ts               # WebSocket client
    ├── hooks/
    │   ├── useAudioRecorder.ts
    │   └── useEvents.ts        # behavior tracking
    └── lib/
```

### 8.2. State management strategy

- **React Query** cho server state (data fetching, caching)
- **Zustand** cho complex client state (active session, recording state)
- **URL state** cho filters (search params trong VideosPage)
- **localStorage** chỉ cho auth token

### 8.3. Key reusable components

- `<VocabChip word cefr onClick />` — dùng ở: video popup, flashcard, retell feedback, speak feedback
- `<AudioRecorder onStop maxDuration />` — dùng ở: phrase, shadow, retell, speak
- `<CEFRBadge level />` — universal
- `<AddToFlashcardDialog vocab contextSentence sourceVideoId />` — global
- `<StepNavigator current completed onNavigate />` — session header

---

## 9. Roadmap 15 tuần

### Phase 1 — Foundation (tuần 1-2)
- Setup repo, docker-compose (postgres, redis, minio)
- Spring Boot skeleton + Auth JWT
- React skeleton + login/register UI
- CI cơ bản

### Phase 2 — Content Layer (tuần 3-4)
- Vocab module + seed Oxford 5000
- Video entity + admin upload
- AI pipeline (admin only): Whisper + key_points + warmup + summary
- Pre-process script chạy 30-50 video mẫu
- Library frontend (browse, filter, search)

### Phase 3 — Core Session (tuần 5-6)
- LearningSession state machine
- Step 0 Warmup + Step 1 Listen (Player + SubtitleOverlay + VocabPopup)
- Flashcard module + FSRS algorithm
- Bước 6 Quick Review

### Phase 4 — Speaking Skills (tuần 7-9)
- Step 2 Phrase Practice (Whisper compare)
- Step 3 Shadowing (Whisper word-diff + phoneme detection heuristic)
- Step 4 Retell — **dành tuần 9 cả tuần cho phần này**:
  - 4 scaffolding components
  - AudioRecorder polished
  - FeedbackPanel với UI đẹp
  - Prompt engineering iterate

### Phase 5 — Speak + Cross-link (tuần 10-11)
- Step 5 Speak mở rộng
- Cross-link: Flashcard → video, Speaking feedback → add vocab
- WebSocket notification cho async tasks

### Phase 6 — Analytics + Recommendation (tuần 12-13)
- Behavior event tracking
- Stats overview + Weekly chart + Phoneme heatmap
- Content-based recommendation
- Vocab priority algorithm
- Daily challenge widget
- Synthetic data + SVD PoC

### Phase 7 — Polish + Report (tuần 14-15)
- UX polish, error handling, loading states
- Bug fix, performance
- Viết báo cáo
- Chuẩn bị demo

---

## 10. Báo cáo đồ án outline

### Chương 1 — Tổng quan đề tài (10-15 trang)
1.1. Lý do chọn đề tài
1.2. Khảo sát hiện trạng (Duolingo, Quizlet, Elsa Speak — phân tích điểm thiếu)
1.3. Mục tiêu nghiên cứu
1.4. Phạm vi đề tài
1.5. Phương pháp tiếp cận
1.6. Đóng góp của đồ án

### Chương 2 — Cơ sở lý thuyết (15-20 trang)
2.1. Khung CEFR (Common European Framework)
2.2. Lý thuyết Comprehensible Input (Krashen)
2.3. Spaced Repetition — thuật toán FSRS
2.4. Speech Recognition với Whisper
2.5. Large Language Models — prompt engineering
2.6. Recommendation Systems (content-based, collaborative, matrix factorization)
2.7. CMU Pronouncing Dictionary

### Chương 3 — Phân tích thiết kế hệ thống (20-25 trang)
3.1. Kiến trúc tổng thể (architecture diagram)
3.2. Use case diagram
3.3. Class diagram (high-level)
3.4. Sequence diagrams cho 3 luồng:
- Video processing pipeline (admin upload)
- Retell session với AI feedback
- Recommendation request
3.5. Database schema (ERD)
3.6. API design
3.7. Cấu trúc folder backend + frontend

### Chương 4 — AI Components (chương "đinh", 20-25 trang)
4.1. AI Orchestration Layer
4.2. Whisper integration — pipeline transcribe
4.3. Prompt engineering chi tiết:
- Video summary + key points
- Retelling evaluation (★ tập trung)
- Speaking feedback
- Examples input/output
4.4. Phoneme weakness detection — heuristic approach
4.5. Caching strategy + cost analysis
4.6. Recommendation Engine:
- Content-based — feature engineering, scoring formula
- Collaborative filtering với SVD — toán + code
- Hybrid approach
- Weakness-based vocab prioritization
4.7. Evaluation: A/B test prompts (nếu có)

### Chương 5 — Cài đặt và kết quả (15-20 trang)
5.1. Môi trường phát triển
5.2. Setup Docker + dependencies
5.3. Screenshots các màn hình chính
5.4. Demo scenario chi tiết
5.5. Testing approach (unit + integration)
5.6. Performance metrics

### Chương 6 — Cost & Optimization (5-10 trang)
6.1. Phân tích chi phí AI per user/day
6.2. Caching strategy giảm cost
6.3. Rate limiting
6.4. Strategy scale: Self-host Whisper, Gemini Flash, freemium model

### Chương 7 — Kết luận và hướng phát triển (5 trang)
7.1. Kết quả đạt được
7.2. Hạn chế:
- Phoneme detection chỉ heuristic
- Collaborative filtering chỉ PoC
- Pronunciation scoring chưa chuyên sâu (Azure)
7.3. Future work:
- User upload video
- YouTube import
- Mobile app
- Pronunciation scoring chuyên sâu
- Social features
- Train custom phoneme model

### Phụ lục
- A. API documentation (auto-gen từ Swagger)
- B. Prompt templates đầy đủ
- C. Database schema SQL
- D. CMU phoneme reference table
- E. References / Bibliography

---

## Tóm tắt scope

**4 module core:** Auth, Video Library, Learning Session (7 bước), Vocab/Flashcard

**3 AI showcase points:**
- Retelling Coach (★ điểm sáng)
- Phoneme weakness detection (heuristic)
- Recommendation Engine (content-based + SVD PoC)

**5 module support:** Shadowing, Speaking, Recommendation, Progress/Analytics, Behavior Tracking

**Out of scope:** User upload, YouTube import, OAuth, mobile, social, payment

**Timeline:** 15 tuần (~ 1 semester)

**Budget AI:** $20-40 USD trong suốt dev + demo (đã tính cả Whisper + GPT-4o-mini + 50 video pre-process)

**Điểm bán hàng với hội đồng:**
1. Tích hợp AI sâu, không chỉ wrap API
2. Có Recommendation Engine (kiến thức CS cổ điển)
3. UX có thiết kế giáo học (Krashen i+1, scaffolding)
4. Có chương báo cáo về cost optimization (business mindset)
5. Demo có "wow moment" rõ ràng (retell coach feedback)
