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
| P-BE3+ | Learning Session, Shadow, Retell, Speak, Recommend | ⏳ TODO |

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
- `db/migration/`: V1–V8 (`V8__add_video_enrichment.sql` — thêm 5 cột TEXT vào videos)
- `seed/oxford_5000.csv` — ~300 từ mẫu pipe-separated `word|cefr_level|pos|ipa|phonemes|definition`

**Files đã tạo (frontend):**
- `src/app/`: `App.tsx`, `providers.tsx` (QueryClient + Router), `router.tsx`
- `src/pages/HomePage.tsx` — gọi `/api/health` và hiện status
- `src/shared/api/client.ts` — axios instance, auto-unwrap `response.data`
- `src/shared/lib/utils.ts` — `cn()` helper

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
├── session/    (TODO BE-3) LearningSession state machine (7 steps)
├── shadow/     (TODO BE-4) ShadowAttempt + phoneme scoring (CMU dict)
├── retell/     (TODO BE-5) RetellAttempt + GPT-4o-mini evaluation ★
├── speak/      (TODO BE-5) SpeakAttempt + AI evaluation
└── recommend/  (TODO BE-6) content-based recommendation
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
1. **P-BE3: Learning Session** — state machine 7 bước (Warmup → Listen → Phrase → Shadow → Retell → Speak → Review)
2. **Khi có OPENAI_API_KEY**: set vào `application-local.yml` → test `POST /api/admin/videos/{id}/process` với video thật → verify subtitles + enrichment (warmup_words, collocations, summary)

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
