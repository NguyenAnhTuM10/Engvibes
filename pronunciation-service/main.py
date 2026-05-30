"""
Pronunciation Analysis Service
Nhận transcript (Whisper output) + target_text → trả về IPA comparison + score + tips
"""
from fastapi import FastAPI
from pydantic import BaseModel
import eng_to_ipa as ipa

app = FastAPI(title="Pronunciation Analysis Service", version="1.0.0")

# ---------------------------------------------------------------------------
# DTO
# ---------------------------------------------------------------------------

class AnalyzeRequest(BaseModel):
    transcript: str          # chuỗi Whisper nhận ra, vd: "tink"
    target_text: str         # từ cần phát âm, vd: "think"
    target_ipa: str | None   # IPA đã tính trước (từ lần attempt trước) — tránh gọi lại


class PhonemeMatchResult(BaseModel):
    position: int
    expected: str        # phoneme đúng
    actual: str | None   # phoneme user thực sự phát âm (None = bị bỏ qua)
    matched: bool
    tip: str | None      # gợi ý sửa lỗi, chỉ có khi matched=False


class WordAnalysisResult(BaseModel):
    word: str
    heard: str | None   # từ Whisper nhận ra tương ứng (None = bị bỏ sót)
    word_ipa: str
    score: int          # 0–100 phoneme accuracy cho từ này


class AnalyzeResponse(BaseModel):
    target_ipa: str
    actual_ipa: str
    phoneme_matches: list[PhonemeMatchResult]
    accuracy_score: int   # 0–100, tỉ lệ phoneme đúng
    fluency_score: int    # 0–100, MVP: dựa vào số phoneme user nói được
    overall_score: int    # 0.7 * accuracy + 0.3 * fluency
    word_analyses: list[WordAnalysisResult] = []  # per-word breakdown


# ---------------------------------------------------------------------------
# Phoneme correction tips

# Key: (expected_phoneme, actual_phoneme) dùng ký hiệu IPA
# ---------------------------------------------------------------------------
TIPS: dict[tuple[str, str], str] = {
    # TH sounds — cực kỳ phổ biến với người Việt
    ("θ", "t"): "For /θ/ ('think'), place your tongue tip lightly BETWEEN your teeth, then blow air. Not behind teeth.",
    ("θ", "d"): "For /θ/ ('think'), tongue between teeth + breathe out. Don't add voice.",
    ("θ", "f"): "For /θ/, use your tongue tip between teeth — not your lower lip on your upper teeth.",
    ("ð", "d"): "For /ð/ ('the', 'this'), tongue between teeth + add voice. Feel it vibrate.",
    ("ð", "z"): "For /ð/, use your tongue between teeth — not behind teeth like /z/.",
    # R vs L
    ("r", "l"): "For /r/, curl your tongue back WITHOUT touching the roof of your mouth.",
    ("l", "r"): "For /l/, tap your tongue tip on the ridge just behind your upper teeth.",
    # V vs B/W
    ("v", "b"): "For /v/, rest your upper TEETH on your lower LIP and push air through. Don't close your lips.",
    ("v", "w"): "For /v/, rest your upper teeth on your lower lip — don't round your lips like /w/.",
    ("w", "v"): "For /w/, round your lips — keep teeth away from your lip.",
    ("f", "p"): "For /f/, upper teeth touch lower lip. Don't close your lips like /p/.",
    # Vowels
    ("æ", "ɛ"): "For /æ/ ('cat'), open your mouth wider and drop your jaw slightly.",
    ("æ", "e"): "For /æ/ ('cat'), mouth more open, tongue lower than /e/.",
    ("ɪ", "iː"): "For short /ɪ/ ('bit'), keep the vowel brief — don't stretch it like 'bee'.",
    ("iː", "ɪ"): "For long /iː/ ('beat'), stretch the vowel — hold it longer.",
    ("ʊ", "uː"): "For short /ʊ/ ('book'), keep it brief and relaxed. Don't round lips so much.",
    ("ɑː", "æ"): "For /ɑː/ ('father'), open your mouth very wide and relax your tongue.",
    ("ʌ", "ɑː"): "For /ʌ/ ('cut'), shorter and less open than /ɑː/.",
    # Consonant clusters
    ("ŋ", "n"): "For /ŋ/ ('sing', 'think'), close the BACK of your throat — tongue touches soft palate, not tooth ridge.",
    ("ʃ", "s"): "For /ʃ/ ('she'), round your lips slightly and push the sound further back in your mouth.",
    ("ʒ", "z"): "For /ʒ/ ('measure'), same as /ʃ/ but add voice.",
    ("tʃ", "t"): "For /tʃ/ ('chair'), combine /t/ then /ʃ/ quickly — don't stop after /t/.",
    ("dʒ", "d"): "For /dʒ/ ('judge'), combine /d/ then /ʒ/ quickly — it's one sound.",
    ("z", "s"): "For /z/ ('zoo'), same mouth position as /s/ but add voice — feel your throat vibrate.",
    ("s", "z"): "For /s/ ('see'), no voice — just push air through. No vibration.",
    ("p", "b"): "For /p/ ('pen'), no voice — just pop air. Keep lips pressed then release.",
    ("b", "p"): "For /b/ ('bed'), add voice — feel vibration in throat.",
    ("t", "d"): "For /t/ ('top'), no voice — tap tongue on ridge then release air.",
    ("d", "t"): "For /d/ ('dog'), add voice while tapping tongue on ridge.",
    ("k", "g"): "For /k/ ('cat'), no voice — press tongue to soft palate then release air.",
    ("g", "k"): "For /g/ ('go'), add voice while pressing tongue to soft palate.",
}


# ---------------------------------------------------------------------------
# IPA helpers
# ---------------------------------------------------------------------------

# Digraphs và diphthongs phải check trước single chars
_DIGRAPHS = ["tʃ", "dʒ", "aɪ", "aʊ", "eɪ", "oʊ", "ɔɪ", "iː", "ɑː", "ɔː", "uː", "ɜː", "eə", "ɪə", "ʊə"]

def text_to_ipa_str(text: str) -> str:
    """Chuyển text tiếng Anh → IPA string dùng eng_to_ipa."""
    words = text.lower().strip().split()
    parts = []
    for word in words:
        # Xóa ký tự không phải chữ
        clean = "".join(c for c in word if c.isalpha())
        if not clean:
            continue
        result = ipa.convert(clean)
        # eng_to_ipa trả "*word" khi không tìm thấy — bỏ dấu *
        parts.append(result.replace("*", ""))
    return " ".join(parts)


def split_ipa_to_phonemes(ipa_string: str) -> list[str]:
    """
    Tách IPA string thành list phoneme riêng lẻ.
    Xử lý digraphs (tʃ, dʒ) và diphthongs (aɪ, eɪ...) trước single chars.
    """
    # Xóa stress markers, dấu phân cách từ
    cleaned = (ipa_string
               .replace("ˈ", "").replace("ˌ", "")
               .replace("/", "").replace("[", "").replace("]", ""))

    phonemes: list[str] = []
    i = 0
    while i < len(cleaned):
        if cleaned[i] in (" ", ".", "-"):
            i += 1
            continue

        matched = False
        # Thử match digraph/diphthong trước
        for d in sorted(_DIGRAPHS, key=len, reverse=True):
            if cleaned[i:i+len(d)] == d:
                phonemes.append(d)
                i += len(d)
                matched = True
                break

        if not matched:
            char = cleaned[i]
            # Bỏ qua length marker 'ː' nếu đứng một mình (đã được xử lý trong digraphs)
            if char != "ː":
                phonemes.append(char)
            i += 1

    return [p for p in phonemes if p.strip()]


def levenshtein_align(expected: list[str], actual: list[str]) -> list[tuple[str | None, str | None]]:
    """
    Căn chỉnh hai chuỗi phoneme bằng Levenshtein edit distance.
    Trả về list (expected_phoneme, actual_phoneme):
      - (A, A) = khớp
      - (A, B) = sai phoneme
      - (A, None) = user bỏ sót phoneme
      - (None, B) = user thêm phoneme thừa
    """
    m, n = len(expected), len(actual)

    # DP table
    dp = [[0] * (n + 1) for _ in range(m + 1)]
    for i in range(m + 1):
        dp[i][0] = i
    for j in range(n + 1):
        dp[0][j] = j
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            cost = 0 if expected[i-1] == actual[j-1] else 1
            dp[i][j] = min(
                dp[i-1][j] + 1,      # deletion
                dp[i][j-1] + 1,      # insertion
                dp[i-1][j-1] + cost  # substitution
            )

    # Traceback
    alignment: list[tuple[str | None, str | None]] = []
    i, j = m, n
    while i > 0 or j > 0:
        if i > 0 and j > 0:
            cost = 0 if expected[i-1] == actual[j-1] else 1
            if dp[i][j] == dp[i-1][j-1] + cost:
                alignment.append((expected[i-1], actual[j-1]))
                i -= 1
                j -= 1
                continue
        if i > 0 and dp[i][j] == dp[i-1][j] + 1:
            alignment.append((expected[i-1], None))  # user bỏ sót
            i -= 1
        else:
            alignment.append((None, actual[j-1]))    # user thêm thừa
            j -= 1

    return list(reversed(alignment))


# ---------------------------------------------------------------------------
# Per-word analysis
# ---------------------------------------------------------------------------

def analyze_per_word(target_text: str, transcript: str) -> list[WordAnalysisResult]:
    """So sánh từng từ trong target với từ Whisper nhận ra."""
    target_words = [w for w in target_text.lower().split() if any(c.isalpha() for c in w)]
    heard_words  = ([w for w in transcript.lower().strip().split() if any(c.isalpha() for c in w)]
                    if transcript.strip() else [])

    alignment = levenshtein_align(target_words, heard_words)
    results: list[WordAnalysisResult] = []

    for (exp_word, act_word) in alignment:
        if exp_word is None:
            continue  # bỏ từ thừa user nói
        exp_clean = "".join(c for c in exp_word if c.isalpha())
        word_ipa  = text_to_ipa_str(exp_clean)

        if act_word is None:
            results.append(WordAnalysisResult(word=exp_word, heard=None, word_ipa=word_ipa, score=0))
            continue

        exp_phones = split_ipa_to_phonemes(word_ipa)
        act_clean  = "".join(c for c in act_word if c.isalpha())
        act_ipa    = text_to_ipa_str(act_clean)
        act_phones = split_ipa_to_phonemes(act_ipa)

        if not exp_phones:
            results.append(WordAnalysisResult(word=exp_word, heard=act_word, word_ipa=word_ipa, score=100))
            continue

        word_align    = levenshtein_align(exp_phones, act_phones)
        w_correct     = sum(1 for e, a in word_align if e is not None and a is not None and e == a)
        w_subs        = sum(1 for e, a in word_align if e is not None and a is not None and e != a)
        w_del         = sum(1 for e, a in word_align if e is not None and a is None)
        w_attempted   = w_correct + w_subs
        w_accuracy    = int(w_correct / w_attempted * 100) if w_attempted > 0 else 0
        w_fluency     = int((len(exp_phones) - w_del) / len(exp_phones) * 100)
        score         = int(w_accuracy * 0.65 + w_fluency * 0.35)
        results.append(WordAnalysisResult(word=exp_word, heard=act_word, word_ipa=word_ipa, score=score))

    return results


# ---------------------------------------------------------------------------
# API endpoints
# ---------------------------------------------------------------------------

@app.post("/analyze", response_model=AnalyzeResponse)
def analyze(req: AnalyzeRequest) -> AnalyzeResponse:
    target_ipa = req.target_ipa if req.target_ipa else text_to_ipa_str(req.target_text)
    actual_ipa = text_to_ipa_str(req.transcript) if req.transcript.strip() else ""

    expected_phonemes = split_ipa_to_phonemes(target_ipa)
    actual_phonemes   = split_ipa_to_phonemes(actual_ipa)

    # Không nhận ra giọng nói gì
    if not actual_phonemes:
        return AnalyzeResponse(
            target_ipa=target_ipa,
            actual_ipa="",
            phoneme_matches=[],
            accuracy_score=0,
            fluency_score=0,
            overall_score=0,
        )

    alignment = levenshtein_align(expected_phonemes, actual_phonemes)

    matches: list[PhonemeMatchResult] = []
    correct       = 0
    substitutions = 0
    deletions     = 0

    for pos, (exp, act) in enumerate(alignment):
        is_match = (exp is not None) and (exp == act)
        if exp is not None and act is not None:
            if is_match:
                correct += 1
            else:
                substitutions += 1
        elif exp is not None and act is None:
            deletions += 1
        # insertions (exp=None, act!=None) — không tính vào score

        tip = None
        if exp and act and not is_match:
            tip = TIPS.get((exp, act)) or f"Practice the phoneme /{exp}/. Listen carefully and try again."
        elif exp and act is None:
            tip = f"The sound /{exp}/ was missing. Make sure to pronounce every sound."

        matches.append(PhonemeMatchResult(
            position=pos,
            expected=exp or "",
            actual=act,
            matched=is_match,
            tip=tip,
        ))

    total_expected = len(expected_phonemes)
    attempted = correct + substitutions  # phonemes user actually produced

    # Accuracy: chất lượng phoneme đã phát âm (correct / attempted) — không phạt deletions
    accuracy = int(correct / attempted * 100) if attempted > 0 else 0

    # Fluency: độ đầy đủ phoneme (penalizes deletions only, không penalize substitutions)
    fluency = int((total_expected - deletions) / total_expected * 100) if total_expected > 0 else 0

    # Overall: weighted, cả hai phải tốt mới cao
    overall = int(accuracy * 0.65 + fluency * 0.35)

    word_analyses = analyze_per_word(req.target_text, req.transcript)

    return AnalyzeResponse(
        target_ipa=target_ipa,
        actual_ipa=actual_ipa,
        phoneme_matches=matches,
        accuracy_score=accuracy,
        fluency_score=fluency,
        overall_score=overall,
        word_analyses=word_analyses,
    )


@app.get("/health")
def health() -> dict:
    return {"status": "UP", "service": "pronunciation-analysis"}
