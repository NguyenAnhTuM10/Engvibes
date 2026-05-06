# 🚀 English Learning App — Bộ Prompts Hoàn Chỉnh (Spring Boot + React TS)

> Đồ án tốt nghiệp web app học tiếng Anh tập trung Listening + Speaking với AI.
> Stack: **Spring Boot 3.3 (Java 21) + React 18 (TypeScript) + PostgreSQL + Redis + MinIO/R2 + OpenAI**

---

## 📦 Bộ files trong outputs

| #   | File                       | Lines | Mục đích                                           | Dùng khi                    |
| --- | -------------------------- | ----- | -------------------------------------------------- | --------------------------- |
| 1   | **CLAUDE.md**              | 315   | Project context cho AI agent đọc trước mỗi session | TRƯỚC mọi prompt            |
| 2   | **SPEC_FULL.md**           | 1341  | Spec đầy đủ (entities, API, business logic)        | Reference khi cần chi tiết  |
| 3   | **SETUP_PROMPTS.md**       | 469   | 5 prompts setup project từ con số 0                | **Tuần 1 (làm trước tiên)** |
| 4   | **BACKEND_PROMPTS.md**     | 1777  | 18 prompts Spring Boot (7 phases)                  | Tuần 1-10                   |
| 5   | **FRONTEND_PROMPTS.md**    | 1720  | 16 prompts React TS (6 phases)                     | Tuần 5-10 (song song BE)    |
| 6   | **DEPLOY_FREE_PROMPTS.md** | 1277  | 6 prompts deploy free tier ($0 cloud)              | Tuần 11-12                  |
| 7   | DEPLOY_PROMPTS.md          | 1095  | VPS deploy alternative (~150k/tháng)               | Backup option               |

**Tổng:** ~8000 lines, ~50 prompts

---

## 🎯 Bắt đầu từ đầu — Workflow 3 bước

### Bước 1: Đặt CLAUDE.md vào root project

```powershell
cd D:\AAA
mkdir englishapp
cd englishapp
git init
```

**Copy file CLAUDE.md** từ outputs vào `D:\AAA\englishapp\CLAUDE.md`.

> ⚠️ Đây là file QUAN TRỌNG NHẤT. Mọi AI agent (Claude Code, Cursor) phải đọc nó trước khi code.

### Bước 2: Setup project (P-SETUP-1 → P-SETUP-5)

Mở **SETUP_PROMPTS.md**, copy từng prompt vào Claude Code:

```
Tuần 1, Ngày 1-2:
P-SETUP-1: Monorepo + docker-compose          ~30 phút
P-SETUP-2: Spring Boot init                   ~1.5h
P-SETUP-3: React + Vite + Tailwind            ~1h
P-SETUP-4: Verify FE → BE → DB integration    ~30 phút
P-SETUP-5: Cleanup demo + GitHub Actions CI   ~30 phút
```

**Sau khi xong:** Backend chạy port 8080, Frontend chạy port 5173, FE call BE OK.

### Bước 3: Build features (P-BE + P-FE)

Mở **BACKEND_PROMPTS.md** và **FRONTEND_PROMPTS.md** song song.

**Phase Map đề xuất:**

| Tuần      | Backend                                           | Frontend                                   |
| --------- | ------------------------------------------------- | ------------------------------------------ |
| **1-2**   | P-BE1-1 → P-BE1-3 (Auth + Vocab + Flashcard)      | —                                          |
| **3-4**   | P-BE2-1 → P-BE2-6 (Video + AI Pipeline)           | —                                          |
| **5**     | P-BE3-1, P-BE3-2 (Learning Session)               | P-FE1-1 → P-FE1-4 (Auth + Layout + Vocab)  |
| **6-7**   | P-BE4-1, P-BE4-2 (Phrase + Shadow)                | P-FE2-1 → P-FE2-3 (Video Library + Listen) |
| **8**     | P-BE5-1 (★ Retell Coach)                          | P-FE3-1, P-FE3-2 (Phrase + Shadow)         |
| **9**     | P-BE5-2 (Speak)                                   | P-FE4-1 → P-FE4-3 (★ Retell + Speak)       |
| **10**    | P-BE6-1 → P-BE6-3 (Recommend + Stats + WS)        | P-FE5-1, P-FE5-2 (Stats + Profile)         |
| **11-12** | **DEPLOY_FREE_PROMPTS.md** (deploy Render/Vercel) | (deploy chung)                             |
| **13-14** | P-BE7-1 (Polish + seed)                           | P-FE6-1, P-FE6-2 (Polish + Demo prep)      |
| **15-16** | Bảo vệ đồ án                                      | —                                          |

---

## 🏗️ Tech Stack chốt

### Backend (IntelliJ IDEA)

- **Java 21 LTS**, **Spring Boot 3.3.x**
- **Gradle Groovy DSL**
- **PostgreSQL 16** + **Flyway** migrations
- **Redis 7** (cache + rate limit)
- **MinIO** local (S3-compatible) → **Cloudflare R2** prod
- **JJWT 0.12** + **Spring Security**
- **Spring WebSocket STOMP**
- **AWS S3 SDK v2**
- **Spring WebFlux** (gọi OpenAI API)
- **Lombok + MapStruct**
- **springdoc-openapi** (Swagger UI)
- **JUnit 5 + Testcontainers**

### Frontend (VS Code)

- **React 18 + TypeScript strict**
- **Vite** (KHÔNG Create React App)
- **TailwindCSS** + **shadcn/ui**
- **React Router v6**, **TanStack Query v5**
- **Zustand** (client state), **React Hook Form + Zod**
- **react-player**, **recharts**, **lucide-react**
- **axios**, **@stomp/stompjs + sockjs-client** (WebSocket STOMP)

### AI & External

- **OpenAI Whisper API** (ASR)
- **OpenAI GPT-4o-mini** (LLM)
- **CMU Pronouncing Dictionary** (phoneme heuristic - free CSV)

### Local Dev Infrastructure

- **Docker Compose**: postgres, redis, minio
- **ffmpeg** (cài trên host)

### Production (Free Tier - $9-13 cho cả đồ án)

- **Render.com** (Backend)
- **Vercel** (Frontend)
- **Neon.tech** (PostgreSQL)
- **Upstash** (Redis)
- **Cloudflare R2** (Object Storage)
- **id.vn** (Free Vietnamese domain)
- **GitHub Actions** (CI/CD)
- **UptimeRobot** (Monitoring)

---

## 📋 Prerequisites — Cài đặt máy

Trước khi bắt đầu, cài:

- [ ] **Java 21 LTS** — `java -version` → `21.x.x`
- [ ] **Node.js 20 LTS** — `node -v` → `v20.x.x`
- [ ] **Docker Desktop** — `docker --version`
- [ ] **Git** — `git --version`
- [ ] **ffmpeg** — `ffmpeg -version` (Windows: `choco install ffmpeg`)
- [ ] **IntelliJ IDEA** Community/Ultimate (cho Java)
- [ ] **VS Code** (cho TypeScript)
- [ ] **Claude Code** hoặc **Cursor** (AI agent)
- [ ] **OpenAI account** với API key (~$10 nạp ban đầu)
- [ ] **Domain englishapp.id.vn** đăng ký free tại id.vn (sau khi gần xong)

---

## 🎓 USP Đồ án — Highlight cho bảo vệ

**AI Retelling Coach (★ Phase BE-5):**

- User xem video 30-60s
- User kể lại bằng tiếng Anh (ghi âm)
- AI đánh giá:
  - **Coverage Score** (0-100): bao nhiều % key points cover được
  - **Vocab Score**: dùng từ vựng target có đúng không
  - **Grammar Score**: ngữ pháp
  - **Improvement Tips**: 2-3 gợi ý cụ thể
  - **Model Answer**: câu trả lời mẫu phù hợp CEFR level
- 4 levels scaffolding (L1=no help → L4=full template)

Đây là **điểm sáng đồ án** — phỏng vấn nhấn mạnh tính năng này.

---

## 🚧 OUT OF SCOPE — Tuyệt đối không làm

Để tránh scope creep:

- ❌ User upload video (chỉ admin upload)
- ❌ YouTube import / scraping
- ❌ OAuth / Google/Facebook login
- ❌ Mobile app
- ❌ Social features (leaderboard, friends)
- ❌ Payment / subscription
- ❌ Email verification / password reset
- ❌ Microservices (single Spring Boot app)
- ❌ GraphQL (REST only)
- ❌ Azure Pronunciation Assessment (dùng Whisper + heuristic CMU dict)

---

## 💰 Cost Summary

| Item             | Local Dev   | Production Free Tier    |
| ---------------- | ----------- | ----------------------- |
| PostgreSQL       | $0 (Docker) | $0 (Neon)               |
| Redis            | $0 (Docker) | $0 (Upstash)            |
| MinIO/R2         | $0 (Docker) | $0 (Cloudflare R2)      |
| Backend hosting  | $0          | $0 (Render)             |
| Frontend hosting | $0          | $0 (Vercel)             |
| Domain           | $0          | $0 (id.vn free)         |
| SSL              | $0          | $0 (auto)               |
| CI/CD            | $0          | $0 (GitHub Actions)     |
| **OpenAI API**   | **~$5 dev** | **~$5 production demo** |
| **TOTAL**        | **~$5**     | **~$10 (250k VND)**     |

---

## 📝 Ghi chú quan trọng cho fresher CV

Sau khi xong đồ án này, CV của bạn sẽ có:

✅ **Project 1 (đã có):** Spring Boot Construction Materials Management

- Spring Security + JWT, RBAC, transactions, layered architecture

✅ **Project 2 (đồ án này):** English Learning Platform

- Spring Boot + React + PostgreSQL + Redis + S3
- AI Integration (Whisper + GPT-4o-mini)
- WebSocket real-time
- FSRS spaced repetition algorithm
- Microservices-ready architecture
- CI/CD pipeline (GitHub Actions)
- Free tier deployment

→ Apply được job: ngân hàng, fintech, FPT, Viettel, BIDV, MB, CMC, AI startup.

**Lương fresher target:** 10-15M (Java backend) / 12-18M (Java fullstack với AI).

---

## 🆘 Khi cần hỗ trợ

- **Stuck ở 1 prompt:** Đọc lại CLAUDE.md để chắc convention chưa lạc
- **AI agent quên context:** Paste lại CLAUDE.md vào đầu session
- **Bug khó:** Đọc Troubleshooting trong từng PROMPTS file
- **Quên thứ tự:** Quay lại Phase Map ở README này

---

## ✅ Checklist trước demo bảo vệ

**1 tuần trước:**

- [ ] Toàn bộ 7 steps flow chạy end-to-end
- [ ] AI Retelling Coach demo được
- [ ] Stats dashboard hiển thị data thật
- [ ] Đã deploy lên https://englishapp.id.vn
- [ ] CI/CD pipeline xanh
- [ ] Backup database (`./deploy/backup-neon.sh`)

**1 ngày trước:**

- [ ] Wake up Render (vào website 10 phút trước)
- [ ] Test full flow với user demo
- [ ] Screenshot dashboards (Render, Vercel, Neon, R2)
- [ ] Chuẩn bị slide bảo vệ

**Demo flow đề xuất (15-20 phút):**

1. Show URL live https://englishapp.id.vn (1 phút)
2. Đăng nhập demo user → dashboard streak (1 phút)
3. Click recommended video → đi qua 7 steps (10 phút)
4. **Highlight Retell Coach feedback ★** (5 phút)
5. Show stats dashboard với phoneme heatmap (3 phút)
6. Show GitHub repo + CI/CD (2 phút)
7. Q&A (5-10 phút)

---

**Chúc bạn làm đồ án tốt! 🎓**



● Bước 1 — Docker (1 lần, giữ chạy nền):                                   
  Set-Location "D:\AAA\Engvibes"                            
  docker compose up -d                                                     
   
  Bước 2 — Backend (terminal 1):                                           
  Set-Location "D:\AAA\Engvibes"                            
  .\run-backend.ps1
  Chờ thấy Started EnglishAppApplication là OK.

  Bước 3 — Frontend (terminal 2):
  Set-Location "D:\AAA\Engvibes\frontend"
  npm run dev