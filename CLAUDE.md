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
| P-SETUP-4 | Verify FE↔BE↔DB (demo module) | ⏳ Pending |
| P-SETUP-5 | Cleanup demo + GitHub Actions CI | ⏳ Pending |
| P-BE1 | User + JWT Auth | ⏳ Pending |

**Files đã tạo (backend):**
- `EnglishAppApplication.java`
- `common/`: `ApiResponse<T>`, `ApiException`, `GlobalExceptionHandler`, `HealthController`
- `config/`: `SecurityConfig` (permit all tạm), `CorsConfig`, `RedisConfig`, `AsyncConfig`, `WebSocketConfig`, `OpenApiConfig`
- `src/main/resources/application.yml` + `application-local.yml` (gitignored)
- `db/migration/V1__init.sql`

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
├── config/     CorsConfig, RedisConfig, WebSocketConfig, OpenApiConfig, AsyncConfig
├── security/   JWT filter, SecurityConfig, JwtService  ← chưa implement
├── common/     ApiResponse<T>, ApiException, GlobalExceptionHandler, HealthController
├── user/       User entity + auth flow  ← chưa implement
├── video/      Video, SubtitleSegment, VideoVocab, VideoSummary
├── pipeline/   Spring Batch — video processing jobs
├── session/    LearningSession state machine (7 steps)
├── flashcard/  FSRS algorithm
├── shadow/     ShadowAttempt + phoneme scoring (CMU dict)
├── retell/     RetellAttempt + GPT-4o-mini evaluation ★
├── speak/      SpeakAttempt + AI evaluation
├── ai/         AIOrchestrationService — tất cả OpenAI calls qua đây
└── storage/    MinioStorageService (S3 abstraction)
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
