# Closed Loop: Pronunciation ↔ Vocabulary SRS

Hai feature trước đây **rời nhau**: luyện phát âm (Pronunciation) và ôn từ vựng
theo spaced repetition (SM-2 SRS). Tính năng này nối chúng thành **một vòng khép
kín tự học** — đây là điểm nhấn khi demo/phỏng vấn.

## Luồng

```
                    ┌─────────────────────────────────────────────┐
                    │            Pronunciation Practice            │
                    │   user thu âm 1 từ → wav2vec2 → per-word     │
                    │   score (0–100, đúng badge: <60 đỏ, ≥80 xanh)│
                    └───────────────┬─────────────────────────────┘
                                    │  POST /analyze (mỗi attempt)
                                    ▼
                    ┌─────────────────────────────────────────────┐
                    │      SoundsToPracticeService.linkAttempt     │
                    └───────────────┬─────────────────────────────┘
            score < 60 (YẾU)        │              score ≥ 60 (ổn/tốt)
        ┌───────────────────────────┴───────────────────────────┐
        ▼                                                        ▼
  chưa có trong deck?                                   đã có trong deck?
   ┌────┴─────┐                                          ┌──────┴───────┐
  CÓ          KHÔNG                                      CÓ             KHÔNG
   │            │                                        │               │
   ▼            ▼                                        ▼               ▼
 ADD card    DEMOTE                                  PROMOTE         (bỏ qua —
 + Review    review SM-2                             review SM-2      chỉ thêm
 due = now   q<3 (1–2):                              q≥3 (≥80→q=5):   khi yếu)
 (vào queue  EF↓, reset reps,                        EF↑, reps++,
  ngay)      interval→1 ngày                         interval giãn (1→6→…)
   │            │                                        │
   └────────────┴───────────► deck "Sounds to practice" ◄┘
                                    │
                                    ▼
                    ┌─────────────────────────────────────────────┐
                    │   SRS review queue (GET /sm2/review/queue)   │
                    │   user ôn lại → phát âm lại → quay vòng       │
                    └─────────────────────────────────────────────┘
```

**Vòng khép kín:** phát âm yếu → tự vào hàng đợi ôn → ôn lại bằng cách *phát âm
lại* → phát âm đúng chính là **một lần review thành công** của SM-2 → interval
giãn ra → từ rời khỏi rotation dày. Không cần user bấm "Good/Easy" thủ công —
chất lượng phát âm thật quyết định lịch ôn.

## Tiêu chí (rõ ràng, deterministic)

Dùng **per-word score** từ `word_analyses` (đúng badge logic của FE):

| Điều kiện | Hành động | SM-2 |
|---|---|---|
| score < 60, **chưa** có trong deck | `ADDED` — thêm card, Review `due = now` | tái dùng default (EF 2.5, interval 0) |
| score < 60, **đã** có trong deck | `DEMOTED` — không tạo trùng | `review(q)` với q∈{1,2} → reset, EF↓ |
| score ≥ 60, **đã** có trong deck | `PROMOTED` | `review(q)` với q∈{3,4,5} (≥80→5) → interval↑ |
| score ≥ 60, **chưa** có trong deck | — (không thêm) | — |

Bỏ qua từ chức năng ngắn (`a/the/to/of…`, len < 3) — hay bị nuốt khi nói nối,
không phải lỗi phát âm thật.

`score → quality` mapping: `≥80→5, 70→4, 60→3, 40→2, <40→1`. **Tái sử dụng
nguyên `Sm2Service.review()` + `Sm2Scheduler`** — không copy công thức SM-2.

## API

| Method | Path | Mô tả |
|---|---|---|
| POST | `/api/pronunciation/sessions/{id}/attempt` | Phát âm. `AttemptResponse` (qua WS) thêm field `soundCardChanges[]` cho biết card nào vừa ADDED/DEMOTED/PROMOTED → FE báo *"Đã thêm 'ship' vào danh sách ôn"*. |
| GET | `/api/sm2/decks/sounds-to-practice` | Deck hệ thống (tự tạo nếu chưa có) + cards như deck thường. Không có endpoint xóa → không cho xóa. |
| GET | `/api/sm2/review/queue?deck_id=` | Hàng đợi ôn (tái dùng SRS sẵn có). |
| POST | `/api/sm2/review/{cardId}` | Review SM-2 (engine được linkAttempt gọi lại). |

Deck "Sounds to practice" là **system deck**: demo dùng 1 deck global
(`ownerId = null`); production sẽ scope theo `ownerId`.

## Bằng chứng test (số THẬT — espeak audio qua pipeline thật)

`test_audio_srs/run_srs_test.sh` chạy 4 kịch bản end-to-end qua HTTP thật,
đọc trạng thái SM-2 trực tiếp từ Postgres:

```
S1  ship + audio 'dog'  → score 0   → ADDED   EF 2.5  interval 0  reps 0  due=now   (queue=['ship'])
S2  ship + audio 'dog'  → score 0   → DEMOTED EF 2.5→1.96  interval 0→1  due→+1d   (no dup card)
S3a ship + audio 'ship' → score 81  → PROMOTED(q=5) EF 1.96→2.06  interval 1  reps→1
S3b ship + audio 'ship' → score 81  → PROMOTED(q=5) EF 2.06→2.16  interval 1→6  due→+6d
S4  see  + audio 'see'  → score 70  → KHÔNG thêm (≥60 & chưa từng yếu)  deck vẫn ['ship']
```

→ Chứng minh: phát âm sai *đẩy* từ vào SRS với due=now; phát âm sai lặp lại
*không* tạo trùng mà hạ EF + giữ interval tối thiểu; phát âm **đúng** đẩy
interval **1 → 6** và due ra xa **6 ngày** = đúng một lần review thành công.

Tạo audio test: `docker exec englishapp-pronunciation espeak-ng -w /tmp/ship.wav "ship"`.
