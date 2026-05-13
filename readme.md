# EngVibes — AI-Powered English Learning Platform

> A full-stack web application for learning English through short videos, combining spaced repetition, AI speech coaching, and real-time conversation practice.

---

## Features

### 7-Step Video Learning Loop
Each video session guides learners through a structured workflow:

| Step | Name | Description |
|---|---|---|
| 0 | **Warmup** | Preview key vocabulary; mark known/unknown words |
| 1 | **Listen** | Watch video with interactive subtitles; click words to add to deck |
| 2 | **Phrase Practice** | Repeat each sentence; Whisper scores pronunciation accuracy |
| 3 | **Shadow** | Segment-by-segment shadowing with phoneme weakness detection |
| 4 | **Retell** ★ | Retell the video in your own words; AI coach evaluates coverage, grammar, and vocabulary |
| 5 | **Speak** | Answer an open-ended question; LLM scores fluency and coherence |
| 6 | **Quick Review** | FSRS spaced repetition review of vocabulary learned during the session |

### AI Conversation Practice
Turn-by-turn roleplay with an AI partner across 5 scenarios (Job Interview, Coffee Shop, Hotel Check-in, Doctor's Appointment, Making Plans). Each AI response includes pre-generated **keyword hints** so learners always have something to say. Full TTS audio playback for the AI's side.

### Spaced Repetition Flashcards
FSRS-4.5 algorithm schedules vocabulary reviews at optimal intervals. Cards are automatically created from videos and linked to phoneme weakness data.

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
- Java 21 + Spring Boot 3.3 (Gradle)
- Spring Security (JWT), Spring WebFlux (OpenAI API calls), Spring WebSocket (STOMP)
- PostgreSQL 16 + Flyway migrations
- Redis 7 (caching, rate limiting)
- MinIO / S3-compatible object storage (audio, video, thumbnails)
- FFmpeg (audio extraction, thumbnail generation)
- OpenAI Whisper (speech-to-text), GPT-4o-mini (evaluation, coaching, TTS)

**Frontend**
- React 18 + TypeScript (strict)
- Vite 5, TailwindCSS, shadcn/ui
- TanStack Query v5, Zustand, React Hook Form + Zod
- Recharts (analytics dashboard)
- @stomp/stompjs (WebSocket client)

**Infrastructure**
- Docker Compose (local: Postgres, Redis, MinIO)
- GitHub Actions (CI)

---

## Architecture

```
React (Vite)  ──────────────────────────────────────────▶  Spring Boot :8080
  TanStack Query (server state)                              │
  Zustand (client state)                                     ├─ PostgreSQL (JPA + Flyway)
  React Hook Form + Zod (forms)                              ├─ Redis (cache + rate limit)
  @stomp/stompjs (real-time notifications)                   ├─ MinIO / S3 (media files)
                                                             └─ OpenAI (Whisper, GPT-4o, TTS)
```

**Backend package layout (package-by-feature):**
```
com.englishapp/
├── user/          Auth + JWT + user profile
├── vocab/         Oxford 5000 vocabulary seed + lookup
├── flashcard/     FSRS-4.5 deck + card management
├── video/         Upload, FFmpeg processing, subtitle segments
├── pipeline/      Async processing: Whisper → NLP → LLM → PUBLISHED
├── ai/            WhisperClient, LlmClient, NlpService, AIOrchestrationService
├── session/       7-step learning session state machine
├── shadow/        Phoneme detection via CMU dictionary
├── retell/        Scaffold L1–L4, AI scoring (coverage/vocab/grammar)
├── speak/         LLM-evaluated open-ended speaking
├── conversation/  AI roleplay: TTS, keyword hints, session summary
├── recommend/     Content-based video recommender
└── stats/         Analytics: weekly, phoneme, vocab growth, XP
```

---

## Key Technical Decisions

**AI Retell Coach** — GPT-4o-mini evaluates the user's retelling against the video's key points, returning structured JSON scores for coverage, vocabulary usage, and grammar. Four scaffold levels (no help → full template) adjust the difficulty.

**FSRS-4.5 Scheduler** — Implements the open-source Free Spaced Repetition Scheduler algorithm for optimal review intervals, replacing simpler SM-2 approaches.

**Phoneme Weakness Detection** — Whisper transcription is word-diff'd against the reference subtitle. Mismatched words are looked up in the CMU Pronouncing Dictionary (~134k entries) to identify recurring weak phonemes across sessions.

**Async Video Pipeline** — `@Async` Spring executor handles the full processing chain (Whisper → NLP enrichment → LLM collocations + summary) without blocking the upload response. Uses `Propagation.NOT_SUPPORTED` on pipeline methods to prevent transaction rollback from swallowing external-service errors.

**AI Caching** — Per-video LLM outputs (summary, key points, warmup words, collocations, speaking questions) are cached in Redis indefinitely. Personalized evaluations (Retell, Speak) are never cached.

---

## Getting Started

### Prerequisites
- Java 21, Node.js 20, Docker Desktop, FFmpeg

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
```

### 2. Start infrastructure
```bash
docker compose up -d
```

### 3. Run backend
```bash
cd backend
./gradlew build -x test
java -jar build/libs/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### 4. Run frontend
```bash
cd frontend
npm install
npm run dev
```

### 5. Seed demo data (optional)
```bash
java -jar build/libs/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local,demo
```

Demo accounts: `demo@englishapp.com / demo123` · `admin1@englishapp.com / admin123`

---

## API Reference

Interactive documentation available at `http://localhost:8080/swagger-ui.html` when the backend is running.

Selected endpoints:

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Register new account |
| POST | `/api/auth/login` | Login, receive JWT |
| GET | `/api/videos` | List published videos (with filters + recommendations) |
| POST | `/api/sessions` | Start or resume a learning session |
| PATCH | `/api/sessions/{id}/step` | Advance to next step |
| POST | `/api/sessions/{id}/retell/attempt` | Submit retell audio for AI scoring |
| POST | `/api/sessions/{id}/speak/attempt` | Submit speaking audio for LLM evaluation |
| POST | `/api/conversation/start` | Start an AI roleplay session |
| POST | `/api/conversation/{id}/turn` | Submit audio turn, receive AI response + hints |
| GET | `/api/stats/overview` | User stats: streak, XP, phoneme errors |
| GET | `/api/recommend/videos` | Personalized video recommendations |

---

## Project Structure

```
Engvibes/
├── backend/                   Spring Boot application
│   ├── src/main/java/         Java source (package-by-feature)
│   ├── src/main/resources/
│   │   ├── db/migration/      Flyway V1–V18 SQL migrations
│   │   └── seed/              oxford_5000.csv vocabulary seed
│   └── build.gradle
├── frontend/                  React + TypeScript application
│   └── src/
│       ├── app/               Router, providers, error boundary
│       ├── features/          Feature modules (auth, session, conversation, …)
│       ├── pages/             Route-level page components
│       └── shared/            API client, types, utilities
└── docker-compose.yml         Postgres, Redis, MinIO
```

---

## License

MIT
