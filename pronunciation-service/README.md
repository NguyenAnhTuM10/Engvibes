# Pronunciation Service & Practice Content

Python FastAPI microservice phân tích phát âm (wav2vec2 → IPA phoneme) **và**
nội dung tĩnh (words/sentences) cho trang *Pronunciation Practice*.

## Kiến trúc phân tích (tóm tắt)

```
audio → wav2vec2-lv-60-espeak-cv-ft → eSpeak IPA → normalize_ipa() → so khớp
target_text → g2p_en → ARPA → ARPA_TO_IPA → IPA
              └────────── levenshtein_align + scoring ──────────┘
```

Bỏ Whisper khỏi bước phân tích vì ASR tự "sửa" `tink → think`, che mất lỗi phát
âm thật. wav2vec2 output phoneme trực tiếp từ audio nên giữ nguyên lỗi.

- `phonemes.py` — logic thuần (inventory IPA, `TIPS`, `normalize_ipa`,
  `split_ipa_to_phonemes`). Không phụ thuộc torch/g2p ⇒ test được mà không load model.
- `main.py` — model wav2vec2 + endpoint `/analyze`, `/health`.

## Nội dung luyện tập

Toàn bộ words + sentences nằm trong **một file tĩnh**:
`backend/src/main/resources/data/pronunciation_content.json`
— backend load 1 lần lúc startup (`PronunciationContentService`), **không gọi
LLM lúc runtime**. Mỗi word có `text`, `targetSound` (IPA âm trọng tâm), `ipa`
(IPA đầy đủ), `exampleSentence`, `group`.

Endpoint phục vụ (JWT):

| Method | Path | Mô tả |
|---|---|---|
| GET | `/api/pronunciation/words?group=` | Words, lọc theo nhóm âm (bỏ trống = tất cả) |
| GET | `/api/pronunciation/sentences?category=` | Sentences, lọc theo category |

IPA của nội dung dùng **đúng inventory `ARPA_TO_IPA`** của service (ví dụ schwa
viết `ʌ`, nguyên âm dài `-y` viết `iː`), để IPA hiển thị khớp với phía target mà
service tự tính bằng g2p — một bảng ký hiệu duy nhất, không lệch khi chấm điểm.

## Nhóm âm — chọn theo lỗi phát âm đặc trưng của người Việt

Tiếng Việt là ngôn ngữ **âm tiết mở, không bật phụ âm cuối**, thiếu nhiều phụ âm
xát/cụm phụ âm của tiếng Anh. Các nhóm dưới đây nhắm thẳng vào những lỗi đó:

### Words

| Nhóm | Âm trọng tâm | Vì sao người Việt hay sai |
|---|---|---|
| **Final Consonants** | `/t d k g p b m n v/` cuối từ | **Lỗi số 1.** Tiếng Việt chỉ có 6 phụ âm cuối không bật hơi (p, t, c, m, n, ng) → người học nuốt/lược bỏ phụ âm cuối: *cat→ca, five→fai, time→tai*. |
| **S / Z Endings** | `/s/ /z/ /ɪz/` | Quy tắc `-s/-es` (số nhiều, ngôi 3, sở hữu) thường bị bỏ; lẫn `/s/` vô thanh ↔ `/z/` hữu thanh: *dogs /z/* vs *cats /s/* vs *buses /ɪz/*. |
| **TH Sounds** | `/θ/ /ð/` | Tiếng Việt **không có** `θ/ð` → thay bằng `t/th/s` hoặc `d/z`: *think→tink, this→dis*. |
| **R / L** | `/r/ /l/` | Hai âm dễ lẫn, và `/r/` cuối/giữa từ bị buông; *right↔light, river↔liver*. |
| **V / W** | `/v/ /w/` | `/v/` (răng-môi) bị đọc thành `/w/` hoặc `/b/`: *very→wery/bery*. |
| **Sibilants** | `/ʃ tʃ dʒ ʒ/` | Âm xát/tắc-xát sau không có hệ thống trong tiếng Việt: *ship↔chip, measure /ʒ/*. |
| **Vowels** | `ɪ iː æ ʌ ɔː oʊ ʊ uː` | Tiếng Việt không phân biệt nguyên âm dài/ngắn căng-chùng như tiếng Anh. |
| **Short vs Long Vowels** | `/æ ʌ ɑː/`, `/ɪ iː/`, `/ʊ uː/` | Cặp tối thiểu tách rõ dài/ngắn: *ship↔sheep, full↔fool, cat↔cut↔cart*. |
| **Clusters** | cụm phụ âm | Tiếng Việt không có cụm phụ âm → bị chèn nguyên âm hoặc lược: *strength, twelfth, months*. |

### Sentences

| Category | Mục đích |
|---|---|
| **Daily Life / Business / IELTS** | Câu giao tiếp/học thuật theo level B1–C1 (giữ nguyên nội dung gốc). |
| **TH Drills** | Câu nhồi `/θ ð/` mật độ cao. |
| **R/L Drills** | Câu nhồi tương phản `/r/`–`/l/` ("The red lorry, the yellow lorry"). |
| **Final Sound Drills** | Câu nhồi phụ âm cuối bật rõ — chống lỗi nuốt âm cuối. |
| **Tongue Twisters** | Líu lưỡi luyện 1 âm cường độ cao (`She sells seashells…`). |
| **Numbers & Dates** | Chỗ hay vấp: *thirteen↔thirty*, năm *1985*, *$3.50*. |

Mỗi câu drill gắn `level` (B1/B2/C1) và `targetSound` (IPA âm trọng tâm).

## Kiểm thử nội dung

`validate_content.py` đọc JSON và validate bằng **chính** `split_ipa_to_phonemes`
+ inventory của service (không load model):

```powershell
cd pronunciation-service
python validate_content.py
```

Kiểm tra: mọi word đủ field; `ipa` parse được; mọi phoneme của `ipa` và của
`targetSound` nằm trong inventory; in bảng đếm coverage mỗi nhóm. Exit code ≠ 0
nếu có lỗi.

Hiện tại: **67 words / 9 nhóm**, **40 sentences / 8 category** — tất cả hợp lệ.

> Windows: `python` console mặc định cp1258 sẽ làm hỏng hiển thị IPA khi pipe.
> Đặt `PYTHONIOENCODING=utf-8` trước khi pipe ra tool khác. Bản thân service và
> backend trả UTF-8 đúng.
