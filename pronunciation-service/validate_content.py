"""
Kiểm thử nội dung tĩnh trang Pronunciation Practice.

Đọc backend/src/main/resources/data/pronunciation_content.json và validate
bằng CHÍNH logic của service (phonemes.py) — KHÔNG load model wav2vec2:

  1. Mọi word đủ field; ipa parse được bằng split_ipa_to_phonemes; mọi phoneme
     của ipa VÀ của target_sound nằm trong PHONEME_INVENTORY (TIPS/ARPA_TO_IPA).
  2. (Cảnh báo) target_sound nên xuất hiện trong ipa của từ.
  3. Sentence: đủ field; targetSound (nếu có) nằm trong inventory.
  4. In bảng đếm word/sentence mỗi nhóm để thấy độ phủ.

Chạy:  python validate_content.py
Exit code != 0 nếu có lỗi cứng (FAIL).
"""
import json
import sys
from pathlib import Path

# Console Windows (cp1258) không encode được IPA — ép UTF-8.
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

from phonemes import PHONEME_INVENTORY, split_ipa_to_phonemes

CONTENT = (Path(__file__).resolve().parent.parent
           / "backend" / "src" / "main" / "resources"
           / "data" / "pronunciation_content.json")

# Required (non-blank) fields. minimalPair là optional (có thể null cho âm không có cặp).
WORD_FIELDS = ("text", "targetSound", "ipa", "exampleSentence", "group", "vi", "commonError", "tip")
SENT_FIELDS = ("text", "level", "category", "vi", "tip")


def phonemes_in_inventory(ipa: str) -> tuple[list[str], list[str]]:
    """Trả (phonemes, phonemes_ngoài_inventory)."""
    phs = split_ipa_to_phonemes(ipa)
    bad = [p for p in phs if p not in PHONEME_INVENTORY]
    return phs, bad


def main() -> int:
    if not CONTENT.exists():
        print(f"FATAL: không tìm thấy {CONTENT}")
        return 2

    data = json.loads(CONTENT.read_text(encoding="utf-8"))
    words = data.get("words", [])
    sentences = data.get("sentences", [])

    errors: list[str] = []
    warnings: list[str] = []

    # ── Words ────────────────────────────────────────────────────────────
    for i, w in enumerate(words):
        tag = f"word[{i}] {w.get('text', '?')!r} ({w.get('group', '?')})"

        missing = [f for f in WORD_FIELDS if not str(w.get(f, "")).strip()]
        if missing:
            errors.append(f"{tag}: thiếu field {missing}")
            continue

        ipa_phs, ipa_bad = phonemes_in_inventory(w["ipa"])
        if not ipa_phs:
            errors.append(f"{tag}: ipa {w['ipa']!r} parse ra rỗng")
        if ipa_bad:
            errors.append(f"{tag}: ipa {w['ipa']!r} chứa phoneme ngoài inventory {ipa_bad}")

        ts_phs, ts_bad = phonemes_in_inventory(w["targetSound"])
        if not ts_phs:
            errors.append(f"{tag}: targetSound {w['targetSound']!r} parse ra rỗng")
        if ts_bad:
            errors.append(f"{tag}: targetSound {w['targetSound']!r} ngoài inventory {ts_bad}")

        # Cảnh báo: target_sound nên nằm trong ipa của từ
        if w["targetSound"] not in w["ipa"]:
            warnings.append(f"{tag}: targetSound /{w['targetSound']}/ không xuất hiện trong ipa /{w['ipa']}/")

    # ── Sentences ────────────────────────────────────────────────────────
    for i, s in enumerate(sentences):
        tag = f"sentence[{i}] ({s.get('category', '?')})"
        missing = [f for f in SENT_FIELDS if not str(s.get(f, "")).strip()]
        if missing:
            errors.append(f"{tag}: thiếu field {missing}")
        if s.get("level") not in ("B1", "B2", "C1"):
            errors.append(f"{tag}: level không hợp lệ {s.get('level')!r}")
        ts = s.get("targetSound")
        if ts:
            _, ts_bad = phonemes_in_inventory(ts)
            if ts_bad:
                errors.append(f"{tag}: targetSound {ts!r} ngoài inventory {ts_bad}")

    # ── Bảng độ phủ ──────────────────────────────────────────────────────
    def coverage(items, key):
        counts: dict[str, int] = {}
        for it in items:
            counts[it[key]] = counts.get(it[key], 0) + 1
        return counts

    print("\n═══ COVERAGE — WORDS ═══")
    print(f"{'Group':<26} {'Count'}")
    wc = coverage(words, "group")
    for g, n in wc.items():
        print(f"  {g:<24} {n}")
    print(f"  {'TOTAL':<24} {len(words)}  ({len(wc)} groups)")

    print("\n═══ COVERAGE — SENTENCES ═══")
    print(f"{'Category':<26} {'Count'}")
    sc = coverage(sentences, "category")
    for c, n in sc.items():
        print(f"  {c:<24} {n}")
    print(f"  {'TOTAL':<24} {len(sentences)}  ({len(sc)} categories)")

    # ── Kết quả ──────────────────────────────────────────────────────────
    print("\n═══ VALIDATION ═══")
    if warnings:
        print(f"⚠️  {len(warnings)} cảnh báo:")
        for w in warnings:
            print(f"    - {w}")
    if errors:
        print(f"❌ {len(errors)} LỖI:")
        for e in errors:
            print(f"    - {e}")
        return 1
    print("✅ Tất cả word/sentence hợp lệ (ipa parse được, target_sound trong inventory).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
