# EngVibes — AI-Powered English Learning Platform

[![Backend CI](https://github.com/NguyenAnhTuM10/Engvibes/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/NguyenAnhTuM10/Engvibes/actions/workflows/backend-ci.yml)
[![Frontend CI](https://github.com/NguyenAnhTuM10/Engvibes/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/NguyenAnhTuM10/Engvibes/actions/workflows/frontend-ci.yml)

> A full-stack web application for learning English through short videos, combining spaced repetition, AI speech coaching, real-time voice conversation and phoneme-level pronunciation feedback.

---

## Features

### 7-Step Video Learning Loop
Each video session guides learners through a structured workflow:

| Step | Name | Description |
|---|---|---|
| 0 | **Warmup** | Preview key vocabulary; mark known/unknown words |
| 1 | **Listen** | Watch video with interactive subtitles; click any word for a quick meaning and add it to a deck |
| 2 | **Phrase Practice** | Repeat each sentence; Whisper transcription is word-diffed against the subtitle |
| 3 | **Shadow** | Segment-by-segment shadowing with phoneme weakness detection |
| 4 | **Retell** ★ | Retell the video in your own words; AI coach evaluates coverage, grammar, and vocabulary |
| 5 | **Speak** | Answer an open-ended question; LLM scores fluency and coherence |
| 6 | **Quick Review** | FSRS review of the vocabulary saved during the session |

### Real-time Voice Conversation
Voice roleplay with an AI partner across 5 scenarios (Job Interview, Coffee Shop, Hotel Check-in, Doctor's Appointment, Making Plans), powered by the OpenAI Realtime API. The browser never talks to OpenAI directly — it connects to a **JWT-authenticated WebSocket proxy** in Spring Boot that owns the session config, enforces a per-session time cap and a daily quota, and records the transcript server-side. After the conversation, the review is graded from that server-observed transcript, not from client data.

### Pronunciation Assessment
Practise single words or sentences. Audio goes through an async pipeline (MinIO → Whisper → Python FastAPI microservice) that converts text to IPA, aligns expected vs. actual phonemes with Levenshtein edit distance, and returns per-phoneme scores and tips. Progress is pushed to the browser over STOMP WebSocket.

### Vocabulary & Spaced Repetition
- **One FSRS-4.5 scheduler for every card.** Cards are either *dictionary cards* (linked to the Oxford word list, with CEFR level and phonemes) or *custom cards* (free-form front/back).
- **Bulk import** of custom cards from pasted text (Quizlet-style), CSV or JSON, with per-line validation errors and case-insensitive de-duplication.
- **Vocabulary games** on any deck: multiple choice, matching and typing recall (typos within edit distance 1 accepted). Answers are graded server-side and fed back into the FSRS schedule.

### Progress Analytics
- Weekly activity breakdown (minutes by activity type)
- Phoneme error heatmap (CMU Pronouncing Dictionary-based)
- Cumulative vocabulary growth by CEFR level
- Streak tracking and XP system

### Content Recommendations
Content-based recommendation engine ranks videos by CEFR match, topic similarity, and user interaction history.

---

## Tech Stack

**Backend**
- Java 21 + Spring Boot 3.3 (Maven)
- Spring Security (stateless JWT), Spring WebFlux `WebClient` (OpenAI + microservice calls), Spring WebSocket (STOMP + raw WebSocket proxy)
- PostgreSQL 16 + Flyway migrations, Spring Data JPA, MapStruct, Lombok
- Redis 7 (caching, rate limiting)
- MinIO / S3-compatible object storage (audio, video, thumbnails)
- FFmpeg (audio extraction, thumbnail generation)
- OpenAI Whisper (speech-to-text), GPT-4o-mini (evaluation, coaching), Realtime API (voice conversation)
- JUnit 5, Mockito, Testcontainers

**Pronunciation service**
- Python 3 + FastAPI, `eng-to-ipa`

**Frontend**
- React 18 + TypeScript (strict)
- Vite 5, TailwindCSS, shadcn/ui
- TanStack Query v5, Zustand, React Hook Form + Zod
- Recharts (analytics dashboard), @stomp/stompjs (WebSocket client)

**Infrastructure**
- Docker Compose (local: Postgres, Redis, MinIO, pronunciation service)
- GitHub Actions CI (backend build + tests incl. Testcontainers; frontend type check + build)

---

## Architecture

```
React (Vite)  ─── REST / STOMP ───────────────────────▶  Spring Boot :8080
  TanStack Query (server state)                            │
  Zustand (client state)                                   ├─ PostgreSQL (JPA + Flyway)
  React Hook Form + Zod (forms)                            ├─ Redis (cache + rate limit)
                                                           ├─ MinIO / S3 (media files)
  ─── WebSocket /ws/conversation ──▶ proxy ──────────────  ├─ OpenAI (Whisper, GPT-4o-mini, Realtime)
                                                           └─ Pronunciation service :8000 (FastAPI)
```

**Backend package layout (package-by-feature):**
```
com.englishapp/
├── user/           Auth + JWT + user profile
├── vocab/          Oxford word list seed + lookup + dictionary fallback
├── flashcard/      Decks, dictionary + custom cards, FSRS-4.5 scheduler, bulk import
├── game/           Vocabulary games (multiple choice, matching, typing recall)
├── video/          Upload, FFmpeg processing, subtitle segments
├── pipeline/       Async processing: Whisper → NLP → LLM → PUBLISHED
├── ai/             WhisperClient, LlmClient, NlpService, AIOrchestrationService
├── session/        7-step learning session state machine
├── shadow/         Phoneme detection via CMU dictionary
├── retell/         Scaffold L1–L4, AI scoring (coverage/vocab/grammar), rate limiting
├── speak/          LLM-evaluated open-ended speaking
├── conversation/   Realtime voice proxy, server-side transcripts, review
├── pronunciation/  Async pronunciation pipeline + WebSocket progress
├── recommend/      Content-based video recommender
└── stats/          Analytics: weekly, phoneme, vocab growth, XP
```

---

## Key Technical Decisions

**AI Retell Coach** — GPT-4o-mini evaluates the user's retelling against the video's key points, returning structured JSON scores for coverage, vocabulary usage, and grammar. Four scaffold levels (no help → full template) adjust the difficulty.

**Server-owned realtime sessions** — The conversation WebSocket proxy injects the scenario instructions itself and drops any `session.update` sent by the client, so users cannot rewrite the AI's prompt. Sessions are capped in length, limited per day via Redis, and transcripts are persisted from what the server observed.

**One spaced-repetition model** — Dictionary cards and custom cards live in the same `user_cards` table and share one FSRS scheduler. A `CHECK` constraint guarantees every card has content (`vocab_id` *or* `front` + `back`), and a partial unique index on `(deck_id, LOWER(front)) WHERE vocab_id IS NULL` prevents duplicate custom cards. Migration `V21` merged an earlier, separate SM-2 system into this model; it is covered by a Testcontainers test that migrates real SM-2 data.

**Phoneme Weakness Detection** — Whisper transcription is word-diffed against the reference subtitle. Mismatched words are looked up in the CMU Pronouncing Dictionary (~134k entries) to identify recurring weak phonemes across sessions.

**Async Video Pipeline** — `@Async` Spring executor handles the full processing chain (Whisper → NLP enrichment → LLM collocations + summary) without blocking the upload response. Uses `Propagation.NOT_SUPPORTED` on pipeline methods to prevent transaction rollback from swallowing external-service errors.

**AI Caching** — Per-video LLM outputs (summary, key points, warmup words, collocations, speaking questions) are cached in Redis indefinitely. Personalized evaluations (Retell, Speak) are never cached.

---

## Getting Started

### Prerequisites
- Java 21, Node.js 20+, Docker Desktop, FFmpeg

### 1. Clone & configure
```bash
git clone https://github.com/NguyenAnhTuM10/Engvibes.git
cd Engvibes
cp .env.example .env
```

Create `backend/src/main/resources/application-local.yml`:
```yaml
app:
  openai:
    api-key: your-openai-api-key   # or leave as "dummy" for mock mode
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/englishapp
    username: englishapp
    password: secret
```

Create `frontend/.env`:
```
VITE_API_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/ws
```

### 2. Start infrastructure
```bash
docker compose up -d
```

### 3. Run backend
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Or build a jar:
```bash
./mvnw package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### 4. Run frontend
```bash
cd frontend
npm install
npm run dev
```

### 5. Seed demo data (optional)
```bash
java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local,demo
```

Demo accounts: `demo@englishapp.com / demo123` · `admin1@englishapp.com / admin123`

### Tests
```bash
cd backend
./mvnw verify    # unit tests + Testcontainers integration tests (needs Docker)
```

---

## API Reference

Interactive documentation available at `http://localhost:8080/swagger-ui.html` when the backend is running.

Selected endpoints:

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Register new account |
| POST | `/api/auth/login` | Login, receive JWT |
| GET | `/api/videos` | List published videos (with filters) |
| POST | `/api/sessions` | Start or resume a learning session |
| PATCH | `/api/sessions/{id}/step` | Advance to next step |
| POST | `/api/sessions/{id}/retell/attempt` | Submit retell audio for AI scoring |
| POST | `/api/sessions/{id}/speak/attempt` | Submit speaking audio for LLM evaluation |
| WS | `/ws/conversation?token=…&scenario=…` | Realtime voice conversation (proxied to OpenAI) |
| POST | `/api/conversation/realtime-review` | Grade a finished conversation from the server transcript |
| POST | `/api/pronunciation/sessions/{id}/attempt` | Submit audio; result pushed over WebSocket |
| GET | `/api/decks` | List the user's decks with card / due counts |
| GET | `/api/decks/{id}/cards/due` | Cards due for review |
| POST | `/api/cards` | Add a dictionary card (`vocabId`) or a custom card (`front` + `back`) |
| POST | `/api/cards/{id}/review` | FSRS review (1 = Again … 4 = Easy) |
| POST | `/api/decks/{id}/import/{text\|csv\|json}` | Bulk import custom cards |
| GET | `/api/games/{multiple-choice\|matching\|typing}?deckId=` | Generate a vocabulary game |
| GET | `/api/stats/overview` | User stats: streak, XP, phoneme errors |
| GET | `/api/recommend/videos` | Personalized video recommendations |

---

## Project Structure

```
Engvibes/
├── backend/                   Spring Boot application (Maven)
│   ├── src/main/java/         Java source (package-by-feature)
│   ├── src/main/resources/
│   │   ├── db/migration/      Flyway V1–V21 SQL migrations
│   │   └── seed/              Oxford word list seed
│   ├── src/test/java/         Unit + Testcontainers integration tests
│   └── pom.xml
├── pronunciation-service/     Python FastAPI — IPA conversion + phoneme alignment
├── frontend/                  React + TypeScript application
│   └── src/
│       ├── app/               Router, providers, error boundary
│       ├── features/          Feature modules (auth, session, conversation, flashcard, …)
│       ├── pages/             Route-level page components
│       └── shared/            API client, types, utilities
└── docker-compose.yml         Postgres, Redis, MinIO, pronunciation service
```

---

## License

MIT
