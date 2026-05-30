"""
Phoneme utilities — pure logic, KHÔNG phụ thuộc torch / transformers / g2p.

Tách ra khỏi main.py để:
  1. main.py import lại (single source of truth — không trùng lặp ký hiệu IPA).
  2. validate_content.py import split_ipa_to_phonemes + inventory để kiểm thử
     nội dung tĩnh MÀ KHÔNG phải load model wav2vec2 (~300MB).

Mọi nội dung tĩnh (words/sentences) PHẢI dùng đúng inventory IPA ở đây.
"""

# ---------------------------------------------------------------------------
# TIPS dict  Key: (expected_ipa, actual_ipa)
# ---------------------------------------------------------------------------

TIPS: dict[tuple[str, str | None], str] = {
    ("θ", "t"): "For /θ/ ('think'), place your tongue tip lightly BETWEEN your teeth, then blow air. Not behind teeth.",
    ("θ", "d"): "For /θ/ ('think'), tongue between teeth + breathe out. Don't add voice.",
    ("θ", "f"): "For /θ/, use your tongue tip between teeth — not your lower lip on your upper teeth.",
    ("θ", "s"): "For /θ/, tongue tip between teeth — don't hiss it from behind the teeth like /s/.",
    ("ð", "d"): "For /ð/ ('the', 'this'), tongue between teeth + add voice. Feel it vibrate.",
    ("ð", "z"): "For /ð/, use your tongue between teeth — not behind teeth like /z/.",
    ("r", "l"): "For /r/, curl your tongue back WITHOUT touching the roof of your mouth.",
    ("l", "r"): "For /l/, tap your tongue tip on the ridge just behind your upper teeth.",
    ("v", "b"): "For /v/, rest your upper TEETH on your lower LIP and push air through. Don't close your lips.",
    ("v", "w"): "For /v/, rest your upper teeth on your lower lip — don't round your lips like /w/.",
    ("v", "f"): "For /v/, same as /f/ but ADD voice — feel your throat vibrate.",
    ("w", "v"): "For /w/, round your lips — keep teeth away from your lip.",
    ("f", "p"): "For /f/, upper teeth touch lower lip. Don't close your lips like /p/.",
    ("æ", "ɛ"): "For /æ/ ('cat'), open your mouth wider and drop your jaw slightly.",
    ("æ", "e"): "For /æ/ ('cat'), mouth more open, tongue lower than /e/.",
    ("ɪ", "iː"): "For short /ɪ/ ('bit'), keep the vowel brief — don't stretch it like 'bee'.",
    ("iː", "ɪ"): "For long /iː/ ('beat'), stretch the vowel — hold it longer.",
    ("ʊ", "uː"): "For short /ʊ/ ('book'), keep it brief and relaxed. Don't round lips so much.",
    ("uː", "ʊ"): "For long /uː/ ('boot'), round your lips firmly and hold the vowel longer.",
    ("ɑː", "æ"): "For /ɑː/ ('father'), open your mouth very wide and relax your tongue.",
    ("ʌ", "ɑː"): "For /ʌ/ ('cut'), shorter and less open than /ɑː/.",
    ("ʌ", "æ"): "For /ʌ/ ('cut'), keep the tongue central — don't spread it forward like /æ/ ('cat').",
    ("ŋ", "n"): "For /ŋ/ ('sing', 'think'), close the BACK of your throat — tongue touches soft palate, not tooth ridge.",
    ("ʃ", "s"): "For /ʃ/ ('she'), round your lips slightly and push the sound further back in your mouth.",
    ("ʒ", "z"): "For /ʒ/ ('measure'), same as /ʃ/ but add voice.",
    ("tʃ", "t"): "For /tʃ/ ('chair'), combine /t/ then /ʃ/ quickly — don't stop after /t/.",
    ("tʃ", "ʃ"): "For /tʃ/ ('chip'), start with a /t/ stop before the /ʃ/ — don't soften it to 'ship'.",
    ("dʒ", "d"): "For /dʒ/ ('judge'), combine /d/ then /ʒ/ quickly — it's one sound.",
    ("z", "s"): "For /z/ ('zoo'), same mouth position as /s/ but add voice — feel your throat vibrate.",
    ("s", "z"): "For /s/ ('see'), no voice — just push air through. No vibration.",
    ("p", "b"): "For /p/ ('pen'), no voice — just pop air. Keep lips pressed then release.",
    ("b", "p"): "For /b/ ('bed'), add voice — feel vibration in throat.",
    ("t", "d"): "For /t/ ('top'), no voice — tap tongue on ridge then release air.",
    ("d", "t"): "For /d/ ('dog'), add voice while tapping tongue on ridge.",
    ("k", "g"): "For /k/ ('cat'), no voice — press tongue to soft palate then release air.",
    ("g", "k"): "For /g/ ('go'), add voice while pressing tongue to soft palate.",
    # Final-consonant deletion — lỗi số 1 của người Việt (tiếng Việt không bật phụ âm cuối)
    ("t", None): "Don't drop the final /t/ — tap your tongue and release a small puff of air at the end.",
    ("d", None): "Don't drop the final /d/ — finish the word with a voiced /d/, not silence.",
    ("k", None): "Don't drop the final /k/ — release the /k/ clearly at the end of the word.",
    ("g", None): "Don't drop the final /g/ — voice the /g/ fully at the end.",
    ("p", None): "Don't drop the final /p/ — close your lips and release the /p/.",
    ("s", None): "Don't drop the final /s/ — let the air hiss out at the very end.",
    ("z", None): "Don't drop the final /z/ — buzz the /z/ at the end, not /s/.",
    ("v", None): "Don't drop the final /v/ — keep your teeth on your lip and voice it.",
    ("m", None): "Don't drop the final /m/ — close your lips and hum the /m/.",
    ("n", None): "Don't drop the final /n/ — finish with the tongue on the ridge.",
}


# ---------------------------------------------------------------------------
# ARPAbet → IPA  (phía target_text dùng g2p_en)
# ---------------------------------------------------------------------------

ARPA_TO_IPA: dict[str, str] = {
    "AA": "ɑː", "AE": "æ",  "AH": "ʌ",  "AO": "ɔː", "AW": "aʊ", "AY": "aɪ",
    "EH": "ɛ",  "ER": "ɜː", "EY": "eɪ", "IH": "ɪ",  "IY": "iː", "OW": "oʊ",
    "OY": "ɔɪ", "UH": "ʊ",  "UW": "uː",
    "B": "b",  "CH": "tʃ", "D": "d",  "DH": "ð",  "F": "f",  "G": "g",
    "HH": "h", "JH": "dʒ", "K": "k",  "L": "l",  "M": "m",  "N": "n",
    "NG": "ŋ", "P": "p",   "R": "r",  "S": "s",  "SH": "ʃ", "T": "t",
    "TH": "θ", "V": "v",   "W": "w",  "Y": "j",  "Z": "z",  "ZH": "ʒ",
}

_DIGRAPHS = ["tʃ", "dʒ", "aɪ", "aʊ", "eɪ", "oʊ", "ɔɪ",
             "iː", "ɑː", "ɔː", "uː", "ɜː", "eə", "ɪə", "ʊə"]

# Inventory hợp lệ cho mọi nội dung tĩnh: tập IPA phoneme mà hệ thống nhận biết.
# = giá trị ARPA_TO_IPA (mọi phoneme target hợp lệ phải nằm trong tập này).
PHONEME_INVENTORY: frozenset[str] = frozenset(ARPA_TO_IPA.values())


# ---------------------------------------------------------------------------
# IPA normalisation  eSpeak → ARPA_TO_IPA inventory
#
# BẪY QUAN TRỌNG: wav2vec2 output eSpeak IPA, g2p output ARPA_TO_IPA IPA.
# Hai bên dùng ký hiệu KHÁC NHAU ở một số điểm. normalize_ipa() map eSpeak
# về inventory của chúng ta để levenshtein_align so sánh được chính xác.
# ---------------------------------------------------------------------------

_ESPEAK_TO_OURS: dict[str, str] = {
    "ɹ": "r",    # eSpeak rhotic ↔ our r  (QUAN TRỌNG NHẤT)
    "ɡ": "g",    # U+0261 script-g → U+0067 latin g
    "ɐ": "ʌ",    # near-open central → gần với ʌ nhất
    "ᵻ": "ɪ",    # near-close central unrounded → ɪ
    "ɵ": "ʊ",    # mid central rounded → ʊ
    "ʔ": "",     # glottal stop → bỏ qua
    "ː": "",     # length marker standalone → bỏ (đã xử lý trong digraphs)
}

# Ký hiệu eSpeak đôi khi strip length marker, cần restore
_ESPEAK_VOWEL_RESTORE: dict[str, str] = {
    "ɑ": "ɑː",   # short ɑ → long ɑː (ARPA_TO_IPA map "AA"→ɑː)
    "ɔ": "ɔː",   # short ɔ → long ɔː (nhưng ɔ trước ɪ = ɔɪ → handle in merge)
    "ɜ": "ɜː",
    "i": "iː",   # eSpeak /i/ = our /iː/
    "u": "uː",
}


def normalize_ipa(ph: str) -> str:
    """Chuẩn hoá 1 eSpeak phoneme token về inventory của ARPA_TO_IPA."""
    # Strip stress / boundary markers
    ph = ph.replace("ˈ", "").replace("ˌ", "").replace("|", "").replace(".", "").strip()
    if not ph:
        return ""
    # Direct substitution
    if ph in _ESPEAK_TO_OURS:
        return _ESPEAK_TO_OURS[ph]
    # Vowel length restoration (chỉ khi standalone, không phải part of digraph)
    if ph in _ESPEAK_VOWEL_RESTORE:
        return _ESPEAK_VOWEL_RESTORE[ph]
    return ph


def merge_affricates(phonemes: list[str]) -> list[str]:
    """Gộp t+ʃ → tʃ và d+ʒ → dʒ nếu model output riêng lẻ."""
    merged: list[str] = []
    i = 0
    while i < len(phonemes):
        if i + 1 < len(phonemes) and phonemes[i] + phonemes[i + 1] in {"tʃ", "dʒ"}:
            merged.append(phonemes[i] + phonemes[i + 1])
            i += 2
        else:
            merged.append(phonemes[i])
            i += 1
    return merged


def split_ipa_to_phonemes(ipa_string: str) -> list[str]:
    """Parse 1 IPA string (từ client, cache, hoặc nội dung tĩnh) → list phoneme."""
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
        for d in sorted(_DIGRAPHS, key=len, reverse=True):
            if cleaned[i:i + len(d)] == d:
                phonemes.append(d)
                i += len(d)
                matched = True
                break
        if not matched:
            ch = cleaned[i]
            if ch != "ː":
                phonemes.append(ch)
            i += 1
    return [p for p in phonemes if p.strip()]