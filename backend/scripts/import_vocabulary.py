#!/usr/bin/env python3
"""
合并多源词库并生成 Flyway SQL 种子（cet4/cet6 高频）。

用法:
  python import_vocabulary.py --cet4 path/to/CET4.json --cet6 path/to/CET6.json
  python import_vocabulary.py   # 仅使用 scripts/data/cet4_core.json

KyleBing json-simple（无 CET4.json 单文件，用 CET4_1.json / CET6_1.json）:
  https://github.com/KyleBing/english-vocabulary/tree/master/json_original/json-simple
  raw: .../json_original/json-simple/CET4_1.json 与 CET6_1.json
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent
DEFAULT_CORE = ROOT / "data" / "cet4_core.json"
DEFAULT_OUT = ROOT.parent / "src" / "main" / "resources" / "db" / "migration" / "V9__seed_vocab_core.sql"

SOURCE_NOTE = "开源词库整理+考纲校验，仅供学习"


def esc(s: str) -> str:
    return s.replace("\\", "\\\\").replace("'", "''")


def brief_meaning(translations: list) -> str:
    parts = []
    for t in translations[:3]:
        if isinstance(t, dict):
            p = t.get("translation") or t.get("cn") or ""
        else:
            p = str(t)
        p = re.sub(r"^[a-z]+\.\s*", "", p.strip())
        if p:
            parts.append(p)
    return "；".join(parts)[:500] or "—"


def load_kylebing(path: Path, exam: str, limit: int) -> list[dict]:
    raw = json.loads(path.read_text(encoding="utf-8"))
    items = []
    rank = 0
    for entry in raw:
        if rank >= limit:
            break
        word = (entry.get("word") or "").strip().lower()
        if not word or len(word) > 64:
            continue
        rank += 1
        trans = entry.get("translations") or entry.get("translation") or []
        if isinstance(trans, str):
            trans = [{"translation": trans}]
        phrases = []
        for p in entry.get("phrases") or []:
            if isinstance(p, dict):
                phrases.append(
                    {"en": p.get("phrase") or p.get("en") or "", "zh": p.get("translation") or p.get("zh") or ""}
                )
        items.append(
            {
                "word": word,
                "phonetic": (entry.get("phonetic") or entry.get("usphone") or "")[:64],
                "meaning_zh": brief_meaning(trans if isinstance(trans, list) else [trans]),
                "part_of_speech": (entry.get("pos") or "")[:16],
                "exam": exam,
                "freq_rank": rank,
                "phrases_json": json.dumps(phrases[:5], ensure_ascii=False) if phrases else None,
            }
        )
    return items


def load_core(path: Path) -> list[dict]:
    raw = json.loads(path.read_text(encoding="utf-8"))
    items = []
    for i, entry in enumerate(raw, start=1):
        items.append(
            {
                "word": entry["word"].strip().lower(),
                "phonetic": entry.get("phonetic", "")[:64],
                "meaning_zh": entry["meaning_zh"][:500],
                "part_of_speech": entry.get("part_of_speech", "")[:16],
                "exam": entry.get("exam", "cet4"),
                "freq_rank": entry.get("freq_rank", i),
                "phrases_json": json.dumps(entry.get("phrases", []), ensure_ascii=False)
                if entry.get("phrases")
                else None,
            }
        )
    return items


def merge(cet4: list[dict], cet6: list[dict]) -> list[dict]:
    seen = set()
    out = []
    for row in cet4 + cet6:
        w = row["word"]
        if w in seen:
            continue
        seen.add(w)
        out.append(row)
    return out


def emit_sql(rows: list[dict], upsert: bool = False) -> str:
    lines = [
        "-- 词汇种子（import_vocabulary.py 生成，勿手改）",
        f"-- 共 {len(rows)} 条",
        "",
    ]
    for r in rows:
        pj = "NULL" if not r.get("phrases_json") else f"'{esc(r['phrases_json'])}'"
        base = (
            "INSERT INTO vocabulary (word, phonetic, part_of_speech, meaning_zh, difficulty, freq_rank, phrases_json, source_note) VALUES "
            f"('{esc(r['word'])}', '{esc(r.get('phonetic') or '')}', '{esc(r.get('part_of_speech') or '')}', "
            f"'{esc(r['meaning_zh'])}', '{r['exam']}', {r['freq_rank']}, {pj}, '{esc(SOURCE_NOTE)}')"
        )
        if upsert:
            lines.append(
                base
                + " ON DUPLICATE KEY UPDATE phonetic=VALUES(phonetic), part_of_speech=VALUES(part_of_speech), "
                "meaning_zh=VALUES(meaning_zh), difficulty=VALUES(difficulty), freq_rank=VALUES(freq_rank), "
                "phrases_json=VALUES(phrases_json), source_note=VALUES(source_note);"
            )
        else:
            lines.append(base + ";")
    return "\n".join(lines) + "\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cet4", type=Path, help="KyleBing CET4 json-simple")
    parser.add_argument("--cet6", type=Path, help="KyleBing CET6 json-simple")
    parser.add_argument("--cet4-limit", type=int, default=600)
    parser.add_argument("--cet6-limit", type=int, default=200)
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    parser.add_argument("--upsert", action="store_true", help="生成 ON DUPLICATE KEY UPDATE（用于 V11+ 扩量）")
    parser.add_argument("--supplement", type=Path, default=ROOT / "data" / "cet4_supplement.json")
    args = parser.parse_args()

    cet4_rows: list[dict] = []
    cet6_rows: list[dict] = []

    if args.cet4 and args.cet4.exists():
        cet4_rows = load_kylebing(args.cet4, "cet4", args.cet4_limit)
    elif DEFAULT_CORE.exists():
        core = load_core(DEFAULT_CORE)
        cet4_rows = [r for r in core if r["exam"] == "cet4"]
        cet6_rows = [r for r in core if r["exam"] == "cet6"]
        if args.supplement.exists():
            extra = load_core(args.supplement)
            cet4_rows.extend([r for r in extra if r["exam"] == "cet4"])
            cet6_rows.extend([r for r in extra if r["exam"] == "cet6"])

    if args.cet6 and args.cet6.exists():
        cet6_rows = load_kylebing(args.cet6, "cet6", args.cet6_limit)

    rows = merge(cet4_rows, cet6_rows)
    if not rows:
        raise SystemExit("无词汇数据：请提供 --cet4/--cet6 或 scripts/data/cet4_core.json")

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(emit_sql(rows, upsert=args.upsert), encoding="utf-8")
    print(f"Wrote {len(rows)} words -> {args.out}")


if __name__ == "__main__":
    main()
