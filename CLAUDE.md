# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **ĐỌC FILE NÀY TRƯỚC mọi session.** Đây là source of truth về tech stack, conventions, bugs đã biết, và tiến độ dự án.

---

## Project Overview

**English Learning Platform** — Đồ án tốt nghiệp CNTT. Web app học Listening + Speaking với AI.

**Core flow:** User chọn video ngắn (30-60s) → 7-step learning loop:
Warmup → Listen → Phrase Practice → Shadow (Whisper) → **Retell (AI coach ★)** → Speak → Quick Review.

**Out of scope — KHÔNG làm:** OAuth/social login, user upload video, YouTube import, mobile app, social/leaderboard, payment, email verification, admin panel phức tạp, Azure Speech, microservices, GraphQL.

---

## Setup Status (tính đến hiện tại)

| Phase | Nội dung | Trạng thái |
|---|---|---|
| P-SETUP-1 | Monorepo, .gitignore, .env.example, docker-compose.yml | ✅ Done |
| P-SETUP-2 | Spring Boot skeleton, application.yml, Flyway V1, HealthController | ✅ Done |
| P-SETUP-3 | React 18 + Vite 5 + Tailwind + path alias + HomePage | ✅ Done |
| P-SETUP-4 | Verify FE↔BE↔DB (demo module) | ✅ Done |
| P-SETUP-5 | Cleanup demo + GitHub Actions CI | ✅ Done |
| P-BE1 | User + JWT Auth + Vocab seed + Flashcard FSRS | ✅ Done |
| P-BE2-1 | Video entity + Admin upload to MinIO + presigned URL | ✅ Done |
| P-BE2-2 | FFmpeg integration — extract audio (16kHz mono) + thumbnail + duration | ✅ Done |
| P-BE2-3 | Whisper client + SubtitleSegment — chờ OPENAI_API_KEY để test thật | ✅ Done (code) |
| P-BE2-4 | Async pipeline + status tracking | ✅ Done |
| P-BE2-5 | NLP enrichment + LLM collocations | ✅ Done |
| P-BE2-6 | LLM video summary + key points + speaking question | ✅ Done |
| P-BE3-1 | LearningSession state machine (7 steps, XP, streak) | ✅ Done |
| P-BE3-2 | Warmup + Listen APIs | ✅ Done |
| P-BE4-1 | Phrase Practice — Whisper + WordDiff scoring | ✅ Done |
| P-BE4-2 | Shadow — Whisper + CMU phoneme detection | ✅ Done |
| P-BE5-1 | Retell Coach — scaffold L1-L4, Whisper, LLM eval, rate limit | ✅ Done |
| P-BE5-2 | Speak — speaking question, Whisper, LLM eval, rate limit | ✅ Done |
| P-BE5-3 | Quick Review — cards from video, FSRS review | ✅ Done |
| P-BE6-1 | Content-based recommendation engine | ✅ Done |
| P-BE6-2 | Stats analytics + behavior events | ✅ Done |
| P-BE6-3 | WebSocket notifications + Rate limiting | ✅ Done |
| P-BE7-1 | Demo seed + OpenAPI polish + README | ✅ Done |
| P-FE1-1 | Auth UI (Login + Register) + JWT store + AuthGuard | ✅ Done |

**Files đã tạo (backend):**
- `EnglishAppApplication.java`
- `common/`: `ApiResponse<T>`, `ApiException`, `GlobalExceptionHandler` (xử lý `AccessDeniedException` → 403), `HealthController`
- `config/`: `SecurityConfig` (JWT, `@EnableMethodSecurity`), `CorsConfig`, `RedisConfig`, `AsyncConfig`, `WebSocketConfig`, `OpenApiConfig`, `AppConfig`, `StorageConfig` (S3Client + S3Presigner)
- `security/`: `JwtService`, `JwtAuthenticationFilter`
- `user/`: `User`, `CEFRLevel`, `Role`, `UserRepository`, `UserService`, `UserMapper`, `AuthController`, `UserController`
- `user/dto/`: `RegisterRequest`, `LoginRequest`, `AuthResponse`, `UserResponse`, `UpdateUserRequest`
- `vocab/`: `VocabEntry`, `VocabRepository`, `VocabMapper`, `VocabSeeder`
- `flashcard/`: `FlashcardDeck`, `UserCard`, `DeckRepository`, `CardRepository`, `FsrsScheduler`, `DeckService`, `CardService`, `DeckController`, `CardController`, `FlashcardMapper`
- `storage/`: `StorageService` (interface — upload/download/presign/delete/exists), `S3StorageService` (MinIO, path-style, auto-create buckets)
- `video/`: `Video`, `VideoStatus`, `VideoRepository`, `VideoService`, `FfmpegService`, `AdminVideoController`, `VideoController`
- `video/dto/`: `CreateVideoRequest`, `UpdateVideoRequest`, `VideoResponse` (incl. enrichment fields), `VideoFilter`, `SubtitleSegmentResponse`
- `video/subtitle/`: `SubtitleSegment`, `SubtitleRepository`, `SubtitleService`
- `pipeline/`: `VideoProcessingPipeline` (`@Async("videoProcessingExecutor")`, `NOT_SUPPORTED`, PROCESSING→PUBLISHED flow incl. enrichment)
- `ai/`: `WhisperClient`, `WhisperResult`, `LlmClient`, `NlpService`, `AIOrchestrationService`
- `ai/dto/`: `WarmupWord`, `VideoEnrichment`, `VideoSummary`
- `db/migration/`: V1–V13 (V8=enrichment, V9=sessions, V10=phrase_attempts, V11=shadow_attempts+phoneme_stats, V12=retell_attempts, V13=speak_attempts)
- `session/`: `LearningSession`, `SessionStatus`, `SessionRepository`, `SessionService`, `SessionController`, `WarmupController`, `ListenController`, `QuickReviewController`
- `session/dto/`: `CreateSessionRequest`, `SessionResponse`, `AdvanceStepRequest`, `SetScaffoldRequest`, `WarmupWordResponse`, `MarkWarmupRequest`, `AddVocabRequest`
- `shadow/`: `ShadowAttempt`, `ShadowAttemptRepository`, `ShadowController`, `WordMatch`, `WordDiffUtil`, `CmuDictService`, `PhonemeDetectionService`, `UserPhonemeStats`, `UserPhonemeStatsId`, `UserPhonemeStatsRepository`
- `retell/`: `RetellAttempt`, `RetellAttemptRepository`, `RetellController`, `RetellService`, `RateLimitService`
- `retell/dto/`: `RetellFeedback`, `RetellScaffoldResponse`, `RetellStartRequest`
- `speak/`: `SpeakAttempt`, `SpeakAttemptRepository`, `SpeakController`
- `speak/dto/`: `SpeakFeedback`, `SpeakingQuestionResponse`
- `resources/data/cmudict.txt` — sample ~200 common words in CMU format (graceful fallback if missing)
- `seed/oxford_5000.csv` — ~300 từ mẫu pipe-separated `word|cefr_level|pos|ipa|phonemes|definition`

**Files đã tạo (frontend):**
- `src/app/`: `App.tsx`, `providers.tsx` (QueryClient + Router + ThemeProvider + Toaster), `router.tsx` (lazy routes)
- `src/lib/utils.ts` — `cn()` cho shadcn (alias `@/lib/utils`)
- `src/shared/lib/utils.ts` — `cn()` helper (legacy)
- `src/shared/api/client.ts` — axios instance; interceptors đặt ở module-level trong providers.tsx
- `src/shared/types/api.ts` — `User`, `AuthResponse`, `LoginData`, `RegisterData`, `ApiResponse<T>`
- `src/features/auth/store.ts` — Zustand persist store (localStorage `auth-storage`): `token`, `user`, `setAuth`, `logout`, `isAuthenticated`
- `src/features/auth/api.ts` — `useLogin`, `useRegister`, `useLogout`, `useCurrentUser` (React Query)
- `src/components/AuthGuard.tsx` — redirect `/login` nếu chưa auth
- `src/components/ui/` — shadcn default style: `button`, `card`, `input`, `label`, `select`, `alert`, `form`, `sonner`
- `src/pages/LoginPage.tsx` — Login form (RHF + zod)
- `src/pages/RegisterPage.tsx` — Register form + CEFR Select
- `src/pages/HomePage.tsx` — Welcome + stat cards + action cards

---

## Dev Commands

### Infrastructure
```powershell
# Start services (Postgres:5433, Redis:6379, MinIO:9000/9001)
docker compose up -d

# Stop giữ data
docker compose down

# Stop + xóa data volumes (reset DB hoàn toàn)
docker compose down -v
```

### Backend
```powershell
# Build (skip tests)
cd backend
.\gradlew.bat build -x test

# Run — LUÔN dùng profile local
java -jar build\libs\backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local

# Chạy từ root Engvibes/ (dotenv không cần)
# Run tests
.\gradlew.bat test

# Run single test class
.\gradlew.bat test --tests "com.englishapp.user.UserControllerTest"
```

### Frontend
```powershell
cd frontend
npm run dev          # dev server :5173
npm run build        # production build
npx tsc --noEmit     # type check
npx vitest run       # run tests once
```

### Verify
```
http://localhost:8080/api/health       → {"data":{"status":"UP",...}}
http://localhost:8080/swagger-ui.html  → Swagger UI
http://localhost:5173                  → React HomePage
http://localhost:9001                  → MinIO Console (minioadmin/minioadmin)
```

---

## Architecture

### Monorepo layout
```
D:\AAA\Engvibes\
├── docker-compose.yml    ← postgres:5433, redis:6379, minio:9000
├── .env.example
├── backend/              ← Spring Boot 3.3 / Java 21 / Gradle Groovy
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/englishapp/
│       └── resources/
│           ├── application.yml           ← env-driven config
│           ├── application-local.yml     ← dev credentials (gitignored)
│           └── db/migration/             ← Flyway V1__, V2__, ...
└── frontend/             ← React 18 + Vite 5 + TypeScript strict
    └── src/
        ├── app/          ← App.tsx, providers.tsx (QueryClient+Router), router.tsx
        ├── pages/        ← Route-level components
        ├── features/     ← Feature modules (auth, videos, session, retell, ...)
        └── shared/       ← api/client.ts, lib/utils.ts, hooks/, types/
```

### Backend package-by-feature
```
com.englishapp/
├── config/     CorsConfig, RedisConfig, WebSocketConfig, OpenApiConfig, AsyncConfig, AppConfig
├── security/   JwtService, JwtAuthenticationFilter
├── common/     ApiResponse<T>, ApiException, GlobalExceptionHandler, HealthController
├── user/       User, UserRepository, UserService, AuthController, UserController, UserMapper
├── vocab/      VocabEntry, VocabRepository, VocabMapper, VocabSeeder (loads seed/oxford_5000.csv on startup)
├── flashcard/  FlashcardDeck, UserCard, DeckRepository, CardRepository, FsrsScheduler (FSRS-4.5)
│               DeckService, CardService, DeckController (/api/decks), CardController (/api/cards)
├── video/      Video, VideoStatus, VideoRepository, VideoService (upload+process+CRUD)
│               FfmpegService (getDuration/extractAudio/extractThumbnail via ProcessBuilder)
│               AdminVideoController (/api/admin/videos — ADMIN, @PreAuthorize)
│               VideoController (/api/videos — PUBLISHED only + viewCount++)
│               subtitle/: SubtitleSegment, SubtitleRepository, SubtitleService (Whisper→groupWords→save)
├── storage/    StorageService (upload/download/presign/delete/exists), S3StorageService (MinIO)
├── ai/         WhisperClient (WebClient → OpenAI /audio/transcriptions, multipart)
│               WhisperResult (text, segments, words với timestamps)
│               LlmClient (WebClient → OpenAI /chat/completions)
│               NlpService (tokenize subtitles → lookup vocab_entries → warmup words)
│               AIOrchestrationService (enrichVideo + generateVideoSummary, Redis cache forever)
│               dto/: WarmupWord, VideoEnrichment, VideoSummary
├── pipeline/   @Async video processing pipeline (Whisper → NLP → LLM → PUBLISHED)
├── session/    LearningSession state machine (7 steps, XP, streak) + WarmupController + ListenController + QuickReviewController
├── shadow/     ShadowAttempt + WordDiffUtil + CmuDictService + PhonemeDetectionService + UserPhonemeStats
├── retell/     RetellAttempt + RetellService (scaffold L1-L4, Whisper, LLM eval) + RateLimitService (Redis INCR)
├── speak/      SpeakAttempt + SpeakController (Whisper + LLM eval, 50/day limit)
└── recommend/  UserVideoInteraction, UserFeatureService, ContentBasedRecommender, RecommendController
```

### Request / data flow
```
React → axios (shared/api/client.ts) → Spring Boot :8080
        React Query cache                ↓
        Zustand (session/recording)    JwtAuthFilter → Service → JPA → Postgres:5433
        React Hook Form + Zod                                  → Redis (cache)
                                                               → MinIO (files)
                                                               → OpenAI (via WebClient, qua ai/)
WebSocket: @stomp/stompjs → /ws (STOMP) → WebSocketConfig
```

---

## Conventions

### Backend
- **Response:** tất cả API trả `ApiResponse<T>` — `{data, message, timestamp}`
- **Exception:** throw `ApiException.notFound("...")` — GlobalExceptionHandler tự xử lý
- **DTO naming:** `CreateXxxRequest`, `UpdateXxxRequest`, `XxxResponse`
- **REST paths:** `/api/{feature}` plural — `/api/videos`, `/api/decks`
- **DB:** UUID PK (`gen_random_uuid()`), soft delete (`deleted_at`), JSONB cho flexible fields
- **Migration:** `V{n}__{description}.sql` — Flyway auto-apply khi boot
- **AI calls:** KHÔNG gọi OpenAI trực tiếp trong service — phải qua `AIOrchestrationService`
- **HTTP client:** WebClient ONLY — KHÔNG dùng RestTemplate
- **Logging:** `@Slf4j` — KHÔNG dùng `System.out.println`
- **Storage:** dùng `StorageService` interface — KHÔNG inject `S3Client` trực tiếp vào service
- **Video ID:** Video entity KHÔNG dùng `@GeneratedValue` — tự generate `UUID.randomUUID()` trong service trước khi upload để dùng làm MinIO key (`videos/{uuid}/source.mp4`)
- **Presigned URL:** expire 1 giờ — generate trong `VideoService.toResponse()`, KHÔNG lưu URL vào DB (chỉ lưu key)
- **Admin role:** promote user bằng psql: `UPDATE users SET role='ADMIN' WHERE email='...';` rồi login lại lấy token mới
- **@EnableMethodSecurity:** đã bật trong `SecurityConfig` — dùng `@PreAuthorize("hasRole('ADMIN')")` trên controller class/method

### Frontend
- **API calls:** LUÔN qua React Query — KHÔNG gọi axios trực tiếp trong component
- **State:** server state → React Query | global → Zustand | form → RHF | URL → search params
- **Styling:** TailwindCSS classes — KHÔNG inline `style={}`
- **Types:** KHÔNG dùng `any` — dùng `unknown` rồi narrow
- **Path alias:** `@/` = `src/`
- **Env vars:** dùng `import.meta.env.VITE_API_URL` — KHÔNG hardcode URL

### AI Caching
| Task | Cache |
|---|---|
| Video summary, key points, warmup, collocations, speaking questions | YES (forever, per video) |
| Whisper transcribe, Retell eval, Speak eval | NO (personalized) |

### Phoneme weakness detection
Dùng CMU Pronouncing Dictionary (CSV ~134k words). Khi shadow: Whisper transcribe → so word-by-word → word sai/thiếu → lookup CMU phonemes → đếm frequency → top 5 = "weak phonemes". Accuracy ~60-70%, đủ cho đồ án. KHÔNG dùng Azure Speech.

---

## Local Dev Config

`application-local.yml` (gitignored) — load bằng `--spring.profiles.active=local`:
- DB: `jdbc:postgresql://localhost:**5433**/englishapp` (port 5433, KHÔNG phải 5432)
- Redis: `redis://localhost:6379`
- MinIO: `http://localhost:9000`

`frontend/.env` (gitignored):
```
VITE_API_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/ws
```

---

## Phiên tiếp theo — TODO & Context cần biết

### Việc cần làm ngay (theo thứ tự)
1. **P-FE1-2** — Main layout + Sidebar navigation (AppLayout, AppHeader, placeholder pages)
2. **P-FE1-3** — Vocab search + Deck list + Deck detail page
3. **P-FE1-4** — Flashcard Review session UI (card flip + FSRS rating)
4. **Khi có OPENAI_API_KEY**: set vào `application-local.yml` → test pipeline thật với video tiếng Anh thật

### Demo profile
- Chạy: `java -jar backend/build/libs/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local,demo`
- Login demo: `demo@englishapp.com / demo123`
- Login admin: `admin1@englishapp.com / admin123`
- Seed: 40 videos, 13 users, 25 sessions, 100 cards, phoneme stats, 50 events

### Trạng thái test BE5 (quan trọng)
Các endpoint **đã test** (không cần API key):
- `GET /retell/start` L1-L4 — scaffold structure đúng ✓
- `GET /speak/question` — trả question + vocab + collocations ✓
- `GET /quick-review` — empty + sau khi add card ✓

Các endpoint **CHƯA test thật** (cần OPENAI_API_KEY + audio file):
- `POST /retell/attempt` — Whisper transcribe → LLM eval → parse `RetellFeedback` JSON
- `POST /speak/attempt` — tương tự, parse `SpeakFeedback` JSON
- Rate limiting Redis (10/day retell, 50/day speak) — chưa có attempt thật nên chưa verify
- `extractJson()` trong SpeakController và RetellService — xử lý LLM trả markdown code block

Tương tự BE2 pipeline: code đúng về logic nhưng integration thật chỉ verify được khi có API key.


### State hiện tại của pipeline (quan trọng)
- `POST /api/admin/videos/{id}/process` trả **202 ngay**, pipeline chạy background trên `videoProcessingExecutor` (2-4 threads)
- Pipeline flow (đầy đủ): `PROCESSING → Whisper → subtitle_segments → NlpService (warmup) → LlmClient (collocations) → LlmClient (summary/keyPoints/speakingQuestion) → PUBLISHED (hoặc FAILED nếu Whisper fail)`
- Enrichment non-critical: nếu LLM fail thì vẫn PUBLISHED, chỉ thiếu enrichment data
- Poll status: `GET /api/admin/videos/{id}/status` → `{id, status, errorMessage}`
- Redis cache: `video:{id}:collocations` và `video:{id}:summary` — no TTL (cache forever)
- Video upload flow: `MultipartFile → temp file → MinIO (source.mp4) → FFmpeg → MinIO (audio.mp3 + thumbnail.jpg) → DB (DRAFT)`

### Bug quan trọng đã gặp — cần nhớ cho phases sau
**Transaction + External Service pattern:**
Bất kỳ method nào vừa gọi external service (Whisper/LLM) vừa cần save FAILED status khi lỗi PHẢI dùng `@Transactional(propagation = Propagation.NOT_SUPPORTED)`. Lý do: nếu để default `@Transactional`, exception từ external call mark transaction rollback-only → catch block save FAILED cũng bị rollback → 500 error. Pattern đúng:
```java
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public Result processWithExternalService(UUID id) {
    setStatus(id, PROCESSING);          // own transaction
    try {
        externalService.call();          // no transaction
        subtitleService.save();          // own transaction (REQUIRES_NEW or default)
    } catch (Exception e) {
        setStatus(id, FAILED, e.msg);   // own transaction — KHÔNG bị rollback
    }
}
```

### Endpoints đã có (summary)
| Method | Path | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/auth/register` | - | Đăng ký |
| POST | `/api/auth/login` | - | Đăng nhập |
| GET | `/api/me` | JWT | Thông tin user |
| PATCH | `/api/me` | JWT | Cập nhật user |
| GET/POST | `/api/decks` | JWT | Flashcard decks |
| PATCH/DELETE | `/api/decks/{id}` | JWT | Sửa/xóa deck |
| GET | `/api/decks/{id}/cards` | JWT | Cards trong deck |
| GET | `/api/decks/{id}/cards/due` | JWT | Cards đến hạn |
| POST | `/api/cards` | JWT | Thêm card |
| POST | `/api/cards/{id}/review` | JWT | Review FSRS |
| DELETE | `/api/cards/{id}` | JWT | Xóa card |
| GET | `/api/videos` | JWT | List PUBLISHED videos |
| GET | `/api/videos/{id}` | JWT | Video detail + view++ |
| GET/POST | `/api/admin/videos` | ADMIN | List all / upload |
| GET/PATCH/DELETE | `/api/admin/videos/{id}` | ADMIN | Quản lý video |
| POST | `/api/admin/videos/{id}/process` | ADMIN | Trigger full pipeline (Whisper + NLP + LLM) |
| GET | `/api/admin/videos/{id}/subtitles` | ADMIN | Subtitle segments với word timings |
| GET | `/api/videos/{id}/subtitles` | JWT | Subtitle segments (PUBLISHED only) |
| POST | `/api/sessions` | JWT | Get or create session (idempotent by user×video) |
| GET | `/api/sessions/{id}` | JWT | Session detail |
| PATCH | `/api/sessions/{id}/step` | JWT | Advance step (complete/skip) |
| PATCH | `/api/sessions/{id}/scaffold` | JWT | Set scaffold level (1-4) |
| POST | `/api/sessions/{id}/finish` | JWT | Finish session, update XP + streak |
| GET | `/api/sessions/history` | JWT | Session history |
| GET | `/api/sessions/{id}/warmup` | JWT | Warmup words with vocabId |
| POST | `/api/sessions/{id}/warmup/mark` | JWT | Mark word known/new (add to deck) |
| GET | `/api/sessions/{id}/listen/subtitles` | JWT | Subtitle segments for listen step |
| POST | `/api/sessions/{id}/listen/add-vocab` | JWT | Add vocab from listen to deck |
| GET | `/api/sessions/{id}/listen/vocab-info?word=` | JWT | Lookup word in vocab_entries |
| POST | `/api/sessions/{id}/phrase/{segmentIdx}/attempt` | JWT | Phrase practice (Whisper compare), max 3 attempts |
| POST | `/api/sessions/{id}/shadow/{segmentIdx}/attempt` | JWT | Shadow attempt (Whisper + phoneme detection), max 3 |
| POST | `/api/sessions/{id}/retell/start` | JWT | Get scaffold (L1-L4) for retell step |
| POST | `/api/sessions/{id}/retell/attempt` | JWT | Submit retell audio (Whisper + LLM eval, 10/day limit) |
| GET | `/api/sessions/{id}/speak/question` | JWT | Speaking question + suggested vocab/collocations |
| POST | `/api/sessions/{id}/speak/attempt` | JWT | Submit speak audio (Whisper + LLM eval, 50/day limit) |
| GET | `/api/sessions/{id}/quick-review` | JWT | Cards added during this video session |
| POST | `/api/sessions/{id}/quick-review/review/{cardId}` | JWT | Review a card (FSRS) |
| GET | `/api/recommend/videos?limit=10` | JWT | Content-based recommended videos |
| GET | `/api/recommend/vocab-priority?limit=20` | JWT | Due cards sorted by phoneme/CEFR priority |
| GET | `/api/recommend/daily-challenge` | JWT | Daily challenge: video + vocab + random phrase |
| GET | `/api/stats/overview` | JWT | streak, xp, videosCompleted, vocabMastered, avgRetellScore7d |
| GET | `/api/stats/weekly` | JWT | Last 7 days activity (minutes + byActivity breakdown) |
| GET | `/api/stats/phonemes` | JWT | Phoneme error rates (min 5 attempts) |
| GET | `/api/stats/vocab-growth` | JWT | Cumulative vocab count by CEFR over time |
| POST | `/api/events` | JWT | Batch track user behavior events (async) |

### VideoResponse enrichment fields (có sau khi PUBLISHED)
```json
{
  "summary": "...",
  "keyPoints": ["...", "..."],
  "speakingQuestion": "...",
  "warmupWords": [{"word":"achieve","ipa":"...","definition":"...","cefrLevel":"B1","partOfSpeech":"verb"}],
  "collocations": {"achieve": ["achieve a goal", "achieve success", "achieve results"]}
}
```

### Cách test full pipeline khi có API key
```bash
# Set OPENAI_API_KEY trong application-local.yml rồi restart
# Upload video tiếng Anh thật (30-60s):
curl -X POST http://localhost:8080/api/admin/videos \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@video.mp4;type=video/mp4" \
  -F 'metadata={"title":"...","topic":"...","cefrLevel":"B1"};type=application/json'

# Trigger processing (202 ngay, pipeline async):
curl -X POST http://localhost:8080/api/admin/videos/{id}/process \
  -H "Authorization: Bearer $TOKEN"

# Poll status:
curl http://localhost:8080/api/admin/videos/{id}/status \
  -H "Authorization: Bearer $TOKEN"

# Verify subtitles:
docker exec englishapp-postgres psql -U englishapp -d englishapp \
  -c "SELECT order_index, start_ms, end_ms, text FROM subtitle_segments WHERE video_id='...' ORDER BY order_index;"

# Verify enrichment:
docker exec englishapp-postgres psql -U englishapp -d englishapp \
  -c "SELECT summary, speaking_question, warmup_words FROM videos WHERE id='...';"
```

---

## Known Bugs & Workarounds (Windows)

### BUG-6: Hibernate field name ≠ DB column name khi dùng suffix `Json`
**Triệu chứng:** `Schema-validation: missing column [collocations_json] in table [videos]` khi boot.  
**Root cause:** Hibernate tự map camelCase → snake_case: `collocationsJson` → `collocations_json`, nhưng migration tạo cột tên `collocations`.  
**Fix đã áp dụng:** Thêm `@Column(name = "collocations")` rõ ràng trên field.  
**Pattern cần nhớ:** Bất kỳ field Java nào có tên khác với DB column name phải dùng `@Column(name = "...")`. Đặc biệt khi field suffix `Json` (Java) ↔ tên ngắn trong DB.



### BUG-1: PostgreSQL port 5432 conflict với Windows native service
**Triệu chứng:** `FATAL: password authentication failed` dù password đúng, kể cả pg_hba.conf dùng `trust`.  
**Root cause:** `postgres.exe` native Windows chiếm port 5432 — Spring Boot kết nối nhầm vào đó.  
**Chẩn đoán:**
```powershell
netstat -ano | findstr ":5432"
tasklist /FI "PID eq <pid>"   # nếu thấy postgres.exe → bị conflict
```
**Fix đã áp dụng:** Docker Postgres map sang `5433:5432` trong `docker-compose.yml`.  
**KHÔNG thử:** sửa pg_hba.conf, xóa volume, hay restart container để fix lỗi này.

### BUG-2: Vite 8+ rolldown crash trên Windows
**Triệu chứng:** `npm run build` crash với `Cannot find module rolldown/dist/shared/binding-...`  
**Root cause:** Vite 8+ dùng rolldown (Rust native binary) — không load được trên một số Windows.  
**Fix đã áp dụng:** Downgrade: `npm install -D vite@^5 @vitejs/plugin-react@^4 --legacy-peer-deps`  
**Lưu ý:** Sau `npm create vite@latest`, luôn check version — nếu v8+ thì downgrade ngay.

### BUG-4: Flyway checksum mismatch sau khi sửa migration đã apply
**Triệu chứng:** `Migration checksum mismatch for migration version N`  
**Root cause:** File `VN__.sql` bị sửa sau khi đã apply vào DB.  
**Fix:** `docker compose down -v && docker compose up -d` (reset DB). Chạy từng lệnh riêng trong PowerShell — `&&` không hợp lệ trong PS 5.1.

### BUG-5: VocabSeeder crash với duplicate CSV entries
**Triệu chứng:** `duplicate key value violates unique constraint "idx_vocab_word_pos"` khi boot.  
**Root cause:** File `oxford_5000.csv` có duplicate entries cho cùng `(word, part_of_speech)` (vd: `explain|verb`). `count() > 0` check chỉ bảo vệ khỏi re-seed toàn bộ, không xử lý duplicate trong file.  
**Fix đã áp dụng:** `VocabSeeder` dùng `LinkedHashMap` với key `word|pos` + `putIfAbsent` để deduplicate trước khi `saveAll`.

### BUG-3: TypeScript `baseUrl` deprecated (TS 6+)
**Triệu chứng:** `tsc -b` lỗi `Option 'baseUrl' is deprecated`.  
**Fix:** Thêm `"ignoreDeprecations": "6.0"` vào `compilerOptions` của `tsconfig.app.json`.

### BUG-7: LazyInitializationException khi MapStruct map LAZY @ManyToOne ngoài transaction
**Triệu chứng:** `GET /api/sessions/{id}/quick-review` → 500 Internal Server Error.  
**Root cause:** `UserCard.vocab` là `@ManyToOne(fetch = FetchType.LAZY)` — khi controller gọi `cardRepository.findByUserIdAndSourceVideoId()` rồi stream-map qua MapStruct, JPA session đã đóng nên proxy không load được.  
**Fix đã áp dụng:** Thay Spring Data method name bằng `@Query("SELECT c FROM UserCard c JOIN FETCH c.vocab WHERE ...")` — buộc eager load vocab trong cùng một query.  
**Pattern cần nhớ:** Bất kỳ query nào trả `List<Entity>` mà entity có LAZY relation và sẽ được access ngay sau đó (không trong @Transactional) → dùng `JOIN FETCH` trong @Query.

### BUG-8: shadcn `base-nova` style tạo file sai thư mục + thiếu `@base-ui/react`
**Triệu chứng:** `npx shadcn@latest add ...` tạo files vào `frontend/@/components/ui/` thay vì `frontend/src/components/ui/`; import dùng `@base-ui/react/button` nhưng package chưa install.  
**Root cause:** (1) shadcn CLI trên Windows không resolve alias `@` → `src/` khi `components.json` dùng `@/components`. (2) `base-nova` style mới yêu cầu `@base-ui/react` chưa có trong registry auto-install.  
**Fix đã áp dụng:** Đổi `"style": "base-nova"` → `"style": "default"` trong `components.json`; đổi aliases thành `"components": "src/components"` rồi sau khi add xong đổi lại về `@/` (để imports trong components dùng `@/lib/utils`). Nếu components tạo `"src/lib/utils"` literal thì dùng `sed` fix thành `"@/lib/utils"`.  
**Pattern cần nhớ khi `shadcn add` lần sau:** Sau khi add, chạy `grep -rn "from \"src/"` trong `src/components/ui/` và fix về `@/` nếu có.

---

## Ambiguity Resolution

Khi không chắc, theo thứ tự:
1. `SPEC_FULL.md` — spec kỹ thuật chi tiết
2. File này (`CLAUDE.md`) — convention & status
3. `BACKEND_PROMPTS.md` / `FRONTEND_PROMPTS.md` — prompt sequence cho từng task
4. Hỏi user

KHÔNG cài dependency ngoài stack đã định nghĩa mà không hỏi. KHÔNG tự refactor code ngoài scope task.

## Git Convention
- Branch: `feature/<phase>-<short-desc>` (vd: `feature/p-be1-jwt-auth`)
- Commit: Conventional Commits — `feat:`, `fix:`, `refactor:`, `test:`, `chore:`, `docs:`
