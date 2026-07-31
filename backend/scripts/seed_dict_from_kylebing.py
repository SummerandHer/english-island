#!/usr/bin/env python3
"""
把 KyleBing CET JSON 补进 dict_entry（补全残缺 ecdict.csv 缺的核心词）。
可与 import_ecdict.py 叠加；重复 word upsert。

用法:
  python seed_dict_from_kylebing.py --mysql
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent


def esc_ok(word: str) -> bool:
    return bool(re.match(r"^[a-z][a-z'-]*$", word)) and len(word) <= 64


def brief(translations) -> str:
    parts = []
    if isinstance(translations, str):
        translations = [{"translation": translations}]
    for t in (translations or [])[:4]:
        if isinstance(t, dict):
            p = t.get("translation") or t.get("cn") or ""
        else:
            p = str(t)
        p = re.sub(r"^[a-z]+\.\s*", "", p.strip(), flags=re.I)
        if p and re.search(r"[\u4e00-\u9fff]", p):
            parts.append(p)
    return "；".join(parts)[:900]


def load_file(path: Path, tags: str) -> list[dict]:
    raw = json.loads(path.read_text(encoding="utf-8"))
    out = []
    for i, entry in enumerate(raw, start=1):
        word = (entry.get("word") or "").strip().lower()
        if not esc_ok(word):
            continue
        zh = brief(entry.get("translations") or entry.get("translation"))
        if not zh:
            continue
        out.append(
            {
                "word": word,
                "phonetic": (entry.get("phonetic") or entry.get("usphone") or "")[:120],
                "meaning_zh": zh,
                "pos": (entry.get("pos") or "")[:60],
                "tags": tags,
                "frq": i,
            }
        )
    return out


def rule_lemmas(word: str) -> list[tuple[str, str, str]]:
    """生成常见变形 → 原型，写入 dict_lemma。"""
    pairs: list[tuple[str, str, str]] = []
    if len(word) < 3:
        return pairs
    # 复数 / 三单
    if word.endswith("y") and len(word) > 3 and word[-2] not in "aeiou":
        pairs.append((word[:-1] + "ies", word, "s"))
    elif word.endswith(("s", "x", "z", "ch", "sh")):
        pairs.append((word + "es", word, "s"))
    else:
        pairs.append((word + "s", word, "s"))
    # -ing / -ed（简易）
    if word.endswith("e") and len(word) > 3:
        pairs.append((word[:-1] + "ing", word, "i"))
        pairs.append((word + "d", word, "d"))
    else:
        pairs.append((word + "ing", word, "i"))
        pairs.append((word + "ed", word, "d"))
    return pairs


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--mysql", action="store_true")
    args = ap.parse_args()
    if not args.mysql:
        print("请加 --mysql", file=sys.stderr)
        sys.exit(1)

    files = [
        (ROOT / "data" / "CET4_1.json", "cet4 kylebing"),
        (ROOT / "data" / "CET6_1.json", "cet6 kylebing"),
    ]
    entries: dict[str, dict] = {}
    for path, tags in files:
        if not path.exists():
            print(f"跳过缺失: {path}")
            continue
        for e in load_file(path, tags):
            entries.setdefault(e["word"], e)
    print(f"词条 {len(entries)}")

    lemmas: dict[str, tuple[str, str]] = {}
    for w in list(entries.keys())[:8000]:
        for variant, lemma, rel in rule_lemmas(w):
            if variant != lemma and esc_ok(variant):
                lemmas.setdefault(variant, (lemma, rel))

    try:
        import pymysql
    except ImportError:
        print("pip install pymysql", file=sys.stderr)
        sys.exit(1)

    conn = pymysql.connect(
        host=os.getenv("MYSQL_HOST", "localhost"),
        port=int(os.getenv("MYSQL_PORT", "3306")),
        user=os.getenv("MYSQL_USER", "root"),
        password=os.getenv("MYSQL_PASSWORD", "12345678"),
        database=os.getenv("MYSQL_DB", "island"),
        charset="utf8mb4",
        autocommit=False,
    )
    try:
        with conn.cursor() as cur:
            sql = (
                "INSERT INTO dict_entry (word, phonetic, meaning_zh, pos, tags, frq, bnc, source) "
                "VALUES (%s,%s,%s,%s,%s,%s,NULL,'kylebing') "
                "ON DUPLICATE KEY UPDATE meaning_zh=IF(source='kylebing' OR meaning_zh IS NULL OR meaning_zh='', VALUES(meaning_zh), meaning_zh), "
                "phonetic=IF(phonetic IS NULL OR phonetic='', VALUES(phonetic), phonetic), "
                "tags=CONCAT_WS(' ', tags, VALUES(tags))"
            )
            batch = [
                (e["word"], e["phonetic"], e["meaning_zh"], e["pos"], e["tags"], e["frq"])
                for e in entries.values()
            ]
            for i in range(0, len(batch), 500):
                cur.executemany(sql, batch[i : i + 500])
                print(f"  entry {min(i+500, len(batch))}/{len(batch)}")

            sql_l = (
                "INSERT INTO dict_lemma (variant, lemma, relation) VALUES (%s,%s,%s) "
                "ON DUPLICATE KEY UPDATE lemma=VALUES(lemma)"
            )
            lbatch = [(v, lem, rel) for v, (lem, rel) in lemmas.items()]
            for i in range(0, len(lbatch), 1000):
                cur.executemany(sql_l, lbatch[i : i + 1000])
                print(f"  lemma {min(i+1000, len(lbatch))}/{len(lbatch)}")
        conn.commit()
        print(f"OK entries={len(entries)} lemmas={len(lemmas)}")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
