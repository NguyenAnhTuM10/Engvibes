# English Learning Platform — Backend

Spring Boot 3.3 / Java 21 / Gradle · REST API for a 7-step AI-powered listening and speaking learning app.

## Architecture

```
com.englishapp/
├── config/         Security, CORS, Redis, WebSocket, Hikari, OpenAPI
├── security/       JWT filter + service
├── common/         ApiResponse<T>, ApiException, GlobalExceptionHandler
├── user/           Auth (register/login) + profile
├── vocab/          VocabEntry seed (Oxford 5000 CSV)
├── flashcard/      Decks + UserCard + FSRS-4.5 scheduler
├── video/          Upload, FFmpeg pipeline, Admin management
├── storage/        S3/MinIO abstraction
├── ai/             WhisperClient, LlmClient, NlpService, AIOrchestrationService
├── pipeline/       Async video processing (Whisper → NLP → LLM → PUBLISHED)
├── session/        7-step LearningSession state machine
├── shadow/         Phoneme detection (CMU dict)
├── retell/         AI retell coach (scaffold L1-L4)
├── speak/          Free speaking with LLM eval
├── recommend/      Content-based recommendation engine
├── stats/          Analytics (XP, streak, phoneme stats, vocab growth)
├── notification/   WebSocket push notifications (STOMP)
└── seed/           Demo data runner (profile: demo)
```

**Request flow:** React → JWT → Spring Security → Service → JPA/Postgres · Redis · MinIO · OpenAI

## Running Locally

**Prerequisites:** Docker Desktop, Java 21, Gradle

```powershell
# 1. Start infrastructure (Postgres:5433, Redis:6379, MinIO:9000)
docker compose up -d

# 2. Build
.\gradlew.bat build -x test

# 3. Run (dev)
java -jar build\libs\backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local

# 4. Verify
# http://localhost:8080/api/health
# http://localhost:8080/swagger-ui.html
```

`application-local.yml` (gitignored) must contain:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/englishapp
    username: englishapp
    password: secret
  data:
    redis:
      url: redis://localhost:6379
app:
  storage:
    endpoint: http://localhost:9000
  openai:
    api-key: YOUR_KEY_HERE
```

## Running Demo Profile

Seeds 40 videos, 13 users, 25 sessions, 100 cards, phoneme stats for the demo user.

```powershell
java -jar build\libs\backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local,demo
# Login: demo@englishapp.com / demo123
# Admin:  admin1@englishapp.com / admin123
```

## API Documentation

Swagger UI: `http://localhost:8080/swagger-ui.html`

OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Endpoint groups (Swagger tags)
| Tag | Base path |
|-----|-----------|
| Authentication | `/api/auth` |
| User | `/api/me` |
| Videos | `/api/videos` |
| Admin - Videos | `/api/admin/videos` |
| Learning Sessions | `/api/sessions` |
| Step 1 - Warmup | `/api/sessions/{id}/warmup` |
| Step 2 - Listen | `/api/sessions/{id}/listen` |
| Step 3 - Phrase Practice | `/api/sessions/{id}/phrases` |
| Step 4 - Shadow | `/api/sessions/{id}/shadow` |
| Step 5 - Retell | `/api/sessions/{id}/retell` |
| Step 6 - Speak | `/api/sessions/{id}/speak` |
| Step 7 - Quick Review | `/api/sessions/{id}/quick-review` |
| Flashcard Decks | `/api/decks` |
| Flashcard Cards | `/api/cards` |
| Recommendations | `/api/recommend` |
| Analytics | `/api/stats` |
| Event Tracking | `/api/events` |

## Common Troubleshooting

**`FATAL: password authentication failed`**
Port conflict with native Windows Postgres. Check `netstat -ano | findstr ":5432"`. The Docker DB runs on 5433.

**`Migration checksum mismatch`**
A migration file was edited after being applied. Reset: `docker compose down -v && docker compose up -d`.

**`Schema-validation: missing column`**
Hibernate maps camelCase to snake_case. If Java field name differs from DB column, add `@Column(name="...")`.

**`LazyInitializationException`**
LAZY relation accessed outside transaction. Add `JOIN FETCH` in the `@Query`.

**Video pipeline returns 500**
External service (Whisper/LLM) failure inside `@Transactional` marks the transaction rollback-only. Methods calling external services must use `@Transactional(propagation = NOT_SUPPORTED)`.
