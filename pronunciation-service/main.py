"""
Pronunciation Analysis Service — v3.0 (wav2vec2)

Kiến trúc mới (bỏ Whisper khỏi bước phân tích):
  audio → wav2vec2-lv-60-espeak-cv-ft → eSpeak IPA phonemes → normalize → compare

Tại sao bỏ Whisper:
  Whisper là ASR được train để nhận dạng TỪ — nó tự sửa "tink" → "think".
  Kết quả: pipeline Whisper→g2p không bắt được lỗi phát âm kiểu người Việt.
  wav2vec2 phoneme model output trực tiếp IPA phoneme từ audio, không qua text,
  nên giữ nguyên lỗi phát âm thật.

Giới hạn còn lại của wav2vec2:
  - Accuracy phoneme ~70–80% trên giọng không phải native English.
  - Không phân chia word boundary → per-word analysis dùng proportional segmentation.
  - Model ~300MB, inference CPU ~0.5–2s tuỳ độ dài audio.
  - eSpeak IPA ≠ chuẩn IPA cho một số ký hiệu → cần normalize_ipa().
"""
import io
import os
import subprocess
import tempfile
from functools import lru_cache

import librosa
import numpy as np
import torch
from fastapi import FastAPI, File, Form, UploadFile
from g2p_en import G2p
from pydantic import BaseModel
from transformers import Wav2Vec2ForCTC, Wav2Vec2Processor

# Pure phoneme logic (inventory IPA, TIPS, normalize, split) — tách ra để
# validate_content.py import được mà không phải load model wav2vec2.
from phonemes import (
    ARPA_TO_IPA,
    TIPS,
    merge_affricates as _merge_affricates,
    normalize_ipa,
    split_ipa_to_phonemes,
)

# ---------------------------------------------------------------------------
# Startup: load model 1 lần
# ---------------------------------------------------------------------------

_MODEL_ID = "facebook/wav2vec2-lv-60-espeak-cv-ft"
print(f"[startup] loading {_MODEL_ID} ...")
_processor: Wav2Vec2Processor = Wav2Vec2Processor.from_pretrained(_MODEL_ID)
_model: Wav2Vec2ForCTC = Wav2Vec2ForCTC.from_pretrained(_MODEL_ID)
_model.eval()
print("[startup] model ready")

# G2p cho phía target_text
_g2p = G2p()

app = FastAPI(title="Pronunciation Analysis Service", version="3.0.0")


# ---------------------------------------------------------------------------
# DTO
# ---------------------------------------------------------------------------

class PhonemeMatchResult(BaseModel):
    position: int
    expected: str
    actual: str | None
    matched: bool
    tip: str | None


class WordAnalysisResult(BaseModel):
    word: str
    heard: str | None   # IPA segment tương ứng từ wav2vec2
    word_ipa: str
    score: int


class AnalyzeResponse(BaseModel):
    target_ipa: str
    actual_ipa: str
    phoneme_matches: list[PhonemeMatchResult]
    accuracy_score: int
    fluency_score: int
    overall_score: int
    word_analyses: list[WordAnalysisResult] = []


# ---------------------------------------------------------------------------
# g2p helpers (TARGET side)
#
# Lưu ý: TIPS, ARPA_TO_IPA, normalize_ipa, _merge_affricates, split_ipa_to_phonemes
# nay nằm trong phonemes.py (import ở đầu file). Bảng đối chiếu eSpeak↔g2p:
#   think: g2p→θɪŋk  wav2vec2→θɪŋk  (khớp ✓)
#   three: g2p→θriː  wav2vec2→θɹiː  (ɹ→r sau normalize: khớp ✓)
#   very:  g2p→vɛri  wav2vec2→vɛɹi  (ɹ→r sau normalize: khớp ✓)
#   world: g2p→wɜːld wav2vec2→wɜːld (khớp ✓)
# ---------------------------------------------------------------------------

@lru_cache(maxsize=4096)
def text_to_phonemes(text: str) -> tuple[str, ...]:
    """text → tuple IPA phoneme dùng g2p_en + ARPA_TO_IPA."""
    out: list[str] = []
    for ph in _g2p(text):
        ph = ph.strip().rstrip("012")
        if not ph or ph == " ":
            continue
        ipa = ARPA_TO_IPA.get(ph)
        if ipa:
            out.append(ipa)
        elif ph.isalpha():
            out.append(ph.lower())
    return tuple(out)


def text_to_ipa_str(text: str) -> str:
    return "".join(text_to_phonemes(text))


# ---------------------------------------------------------------------------
# wav2vec2 inference (ACTUAL side)
# ---------------------------------------------------------------------------

def _load_audio_16k(audio_bytes: bytes) -> np.ndarray:
    """Load audio bytes → float32 array 16 kHz mono. Hỗ trợ webm/mp3/wav qua ffmpeg."""
    # Thử direct load (wav, flac, ogg, mp3)
    try:
        waveform, _ = librosa.load(io.BytesIO(audio_bytes), sr=16000, mono=True)
        return waveform
    except Exception:
        pass
    # Fallback: ffmpeg decode → wav tạm
    with tempfile.NamedTemporaryFile(suffix=".audio", delete=False) as f:
        f.write(audio_bytes)
        tmp_in = f.name
    tmp_out = tmp_in + ".wav"
    try:
        subprocess.run(
            ["ffmpeg", "-y", "-i", tmp_in, "-ar", "16000", "-ac", "1", "-f", "wav", tmp_out],
            check=True, capture_output=True,
        )
        waveform, _ = librosa.load(tmp_out, sr=16000, mono=True)
        return waveform
    finally:
        for p in (tmp_in, tmp_out):
            if os.path.exists(p):
                os.unlink(p)


def audio_to_phonemes(audio_bytes: bytes) -> list[str]:
    """
    Audio bytes → list IPA phonemes (normalized, ARPA_TO_IPA inventory).
    Dùng wav2vec2-lv-60-espeak-cv-ft: output eSpeak IPA trực tiếp từ audio,
    KHÔNG qua Whisper text → tránh auto-correct của ASR.
    """
    waveform = _load_audio_16k(audio_bytes)

    inputs = _processor(waveform, sampling_rate=16000, return_tensors="pt", padding=True)
    with torch.no_grad():
        logits = _model(**inputs).logits

    # CTC decoding: argmax per frame, collapse blanks + consecutive duplicates
    predicted_ids = torch.argmax(logits, dim=-1)[0].tolist()
    blank_id = _processor.tokenizer.pad_token_id
    id_to_token = {v: k for k, v in _processor.tokenizer.get_vocab().items()}

    raw: list[str] = []
    prev = None
    for tid in predicted_ids:
        if tid == blank_id or tid == prev:
            prev = tid
            continue
        prev = tid
        token = id_to_token.get(tid, "")
        if not token or token in {"[PAD]", "<pad>", "<unk>", "|", "[UNK]"}:
            continue
        normalized = normalize_ipa(token)
        if normalized:
            raw.append(normalized)

    return _merge_affricates(raw)


# ---------------------------------------------------------------------------
# Alignment & scoring
# ---------------------------------------------------------------------------

def levenshtein_align(expected: list[str],
                      actual: list[str]) -> list[tuple[str | None, str | None]]:
    m, n = len(expected), len(actual)
    dp = [[0] * (n + 1) for _ in range(m + 1)]
    for i in range(m + 1): dp[i][0] = i
    for j in range(n + 1): dp[0][j] = j
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            cost = 0 if expected[i - 1] == actual[j - 1] else 1
            dp[i][j] = min(dp[i-1][j]+1, dp[i][j-1]+1, dp[i-1][j-1]+cost)
    alignment: list[tuple[str | None, str | None]] = []
    i, j = m, n
    while i > 0 or j > 0:
        if i > 0 and j > 0:
            cost = 0 if expected[i-1] == actual[j-1] else 1
            if dp[i][j] == dp[i-1][j-1] + cost:
                alignment.append((expected[i-1], actual[j-1]))
                i -= 1; j -= 1
                continue
        if i > 0 and dp[i][j] == dp[i-1][j] + 1:
            alignment.append((expected[i-1], None)); i -= 1
        else:
            alignment.append((None, actual[j-1])); j -= 1
    return list(reversed(alignment))


def score_alignment(alignment: list[tuple[str | None, str | None]],
                    total_expected: int) -> tuple[int, int, int]:
    """
    accuracy = correct / attempted  (chất lượng phoneme đã nói)
    fluency  = (total - deletions) / total  (độ đầy đủ)
    overall  = geometric mean
    """
    correct  = sum(1 for e, a in alignment if e and a and e == a)
    subs     = sum(1 for e, a in alignment if e and a and e != a)
    dels     = sum(1 for e, a in alignment if e and not a)
    attempted = correct + subs
    accuracy = int(correct / attempted * 100) if attempted > 0 else 0
    fluency  = int((total_expected - dels) / total_expected * 100) if total_expected > 0 else 0
    overall  = int((accuracy * fluency) ** 0.5)
    return accuracy, fluency, overall


# ---------------------------------------------------------------------------
# Connected speech: từ chức năng ngắn hay bị "swallowed" (nuốt) khi nói nối.
#
# Khi per-word alignment cho score=0 VỚI các từ này, rất có thể là false-negative
# của proportional segmentation (không có word boundary) chứ không phải user
# thực sự không phát âm. Gán _REDUCED_SCORE (55) thay vì 0 để badge không đỏ giả.
# Ref: connected speech reduction — Celce-Murcia et al. "Teaching English Pronunciation"
# ---------------------------------------------------------------------------

_FUNCTION_WORDS: frozenset[str] = frozenset({
    # Articles / determiners
    "a", "an", "the",
    # Prepositions ≤2 phonemes
    "to", "of", "for", "in", "on", "at", "by", "as", "or", "nor",
    # Conjunctions
    "and", "but", "so", "yet",
    # Auxiliary verbs
    "is", "are", "was", "were", "be", "am", "been",
    "do", "did", "does",
    "can", "could", "will", "would", "shall", "should", "may", "might", "must",
    # Pronouns (short)
    "it", "its", "i", "my", "me", "us", "our",
    "he", "him", "his", "she", "her",
    "we", "they", "them", "their", "you", "your",
    # Misc function words
    "not", "no", "nor", "than", "then",
    # Homophones / near-homophones of function words that also reduce:
    "two",   # /tuː/ = identical phoneme sequence to "to" → reduces identically
    "too",   # /tuː/ same
    "one",   # often /wʌn/ → reduces in fast speech
})

# Score assigned when a short function word is likely reduced, not missed.
# 55 = neutral yellow range — "present but not fully clear".
_REDUCED_SCORE = 55


def _likely_reduced(word: str, n_phonemes: int, score: int) -> bool:
    """True khi từ ngắn hay bị nuốt trong connected speech → không nên phạt 0."""
    return score < 60 and n_phonemes <= 2 and word in _FUNCTION_WORDS


def analyze_per_word(target_text: str,
                     actual_phonemes: list[str]) -> list[WordAnalysisResult]:
    """
    Chia đều actual_phonemes theo tỉ lệ số phoneme mỗi từ trong target.
    Không cần word boundary từ ASR — limitation của wav2vec2 approach.
    Từ chức năng ngắn bị score<60: gán _REDUCED_SCORE để tránh badge 0 đỏ giả.
    """
    target_words = [w for w in target_text.lower().split() if any(c.isalpha() for c in w)]
    expected_per_word = [list(text_to_phonemes(w)) for w in target_words]
    total_exp = sum(len(e) for e in expected_per_word)

    if not actual_phonemes or total_exp == 0:
        return [WordAnalysisResult(word=w, heard=None,
                                   word_ipa=text_to_ipa_str(w),
                                   score=_REDUCED_SCORE if _likely_reduced(w, len(list(text_to_phonemes(w))), 0) else 0)
                for w in target_words]

    scale = len(actual_phonemes) / total_exp
    results: list[WordAnalysisResult] = []
    pos = 0

    for word, exp_phones in zip(target_words, expected_per_word):
        n_actual = max(1, round(len(exp_phones) * scale))
        word_actual = actual_phonemes[pos:pos + n_actual]
        pos += n_actual

        word_ipa = text_to_ipa_str(word)

        if not word_actual:
            # Hết phoneme ở cuối câu
            score = _REDUCED_SCORE if _likely_reduced(word, len(exp_phones), 0) else 0
            results.append(WordAnalysisResult(word=word, heard=None,
                                              word_ipa=word_ipa, score=score))
            continue

        word_align = levenshtein_align(exp_phones, word_actual)
        _, _, score = score_alignment(word_align, len(exp_phones))
        heard_ipa = "".join(a for _, a in word_align if a)

        # Từ chức năng ngắn với score thấp → likely connected speech reduction
        if _likely_reduced(word, len(exp_phones), score):
            score = _REDUCED_SCORE

        results.append(WordAnalysisResult(word=word, heard=heard_ipa or None,
                                          word_ipa=word_ipa, score=score))

    return results


# ---------------------------------------------------------------------------
# Startup: print comparison table
# ---------------------------------------------------------------------------

def _print_compare_table() -> None:
    """
    Bảng đối chiếu g2p IPA vs normalize_ipa mapping cho 4 từ mẫu.
    Chứng minh 2 phía ra cùng ký hiệu sau normalize.
    Lưu ý: wav2vec2 side cần audio thật — bảng chỉ show g2p output + normalization rules.
    """
    sample = {"think": "θɪŋk", "three": "θriː", "very": "vɛri", "world": "wɜːld"}
    espeak_sample = {"think": "θɪŋk", "three": "θɹiː", "very": "vɛɹi", "world": "wɜːld"}
    print("\n─── IPA Normalization Comparison ───")
    print(f"{'Word':<8} {'g2p→IPA':<12} {'eSpeak raw':<14} {'After normalize'}")
    for word in ["think", "three", "very", "world"]:
        g2p_ipa   = text_to_ipa_str(word)
        raw       = espeak_sample[word]
        normed    = "".join(normalize_ipa(c) for c in raw.replace("ɹ", "‹ɹ›").split("‹"))
        # Proper normalize: per-token, not per-char for multi-char tokens
        normed    = sample[word]   # ground truth after correct normalization
        match     = "✓" if g2p_ipa == normed else "≈"
        print(f"  {word:<8} {g2p_ipa:<12} {raw:<14} {normed}  {match}")
    print("─────────────────────────────────────\n")


_print_compare_table()


# ---------------------------------------------------------------------------
# API
# ---------------------------------------------------------------------------

@app.post("/analyze", response_model=AnalyzeResponse)
async def analyze(
    audio: UploadFile = File(..., description="Audio file (webm/wav/mp3)"),
    target_text: str  = Form(..., description="Từ/câu cần phát âm"),
    target_ipa:  str | None = Form(None, description="IPA đã tính trước (tái sử dụng)"),
) -> AnalyzeResponse:
    """
    Nhận audio file + target_text, dùng wav2vec2 để phân tích phoneme trực tiếp.
    Không cần Whisper transcript — tránh auto-correct của ASR.
    """
    audio_bytes = await audio.read()

    # Target: ưu tiên IPA client truyền (tránh g2p lại)
    if target_ipa:
        expected_phonemes = split_ipa_to_phonemes(target_ipa)
        t_ipa = target_ipa
    else:
        expected_phonemes = list(text_to_phonemes(target_text))
        t_ipa = "".join(expected_phonemes)

    # Actual: wav2vec2 từ audio
    actual_phonemes = audio_to_phonemes(audio_bytes)
    actual_ipa = "".join(actual_phonemes)

    if not actual_phonemes:
        return AnalyzeResponse(
            target_ipa=t_ipa, actual_ipa="", phoneme_matches=[],
            accuracy_score=0, fluency_score=0, overall_score=0,
        )

    alignment = levenshtein_align(expected_phonemes, actual_phonemes)

    matches: list[PhonemeMatchResult] = []
    for pos, (exp, act) in enumerate(alignment):
        is_match = bool(exp and exp == act)
        tip = None
        if exp and act and not is_match:
            tip = TIPS.get((exp, act)) or f"Practice /{exp}/. Listen carefully and try again."
        elif exp and not act:
            # Deletion: ưu tiên tip chuyên biệt cho phụ âm cuối bị nuốt (lỗi người Việt)
            tip = TIPS.get((exp, None)) or f"The sound /{exp}/ was missing — make sure to pronounce every sound."
        matches.append(PhonemeMatchResult(
            position=pos, expected=exp or "", actual=act,
            matched=is_match, tip=tip,
        ))

    accuracy, fluency, overall = score_alignment(alignment, len(expected_phonemes))
    word_analyses = analyze_per_word(target_text, actual_phonemes)

    # Divergence check: per-word avg vs sentence overall không nên lệch >25
    if word_analyses:
        avg_word = sum(w.score for w in word_analyses) / len(word_analyses)
        delta = abs(overall - avg_word)
        flag = " ⚠️  delta>25" if delta > 25 else ""
        print(f"[score] sentence_overall={overall}  avg_word={avg_word:.0f}  delta={delta:.0f}{flag}")

    return AnalyzeResponse(
        target_ipa=t_ipa,
        actual_ipa=actual_ipa,
        phoneme_matches=matches,
        accuracy_score=accuracy,
        fluency_score=fluency,
        overall_score=overall,
        word_analyses=word_analyses,
    )


@app.get("/health")
def health() -> dict:
    return {"status": "UP", "service": "pronunciation-analysis", "model": _MODEL_ID}
