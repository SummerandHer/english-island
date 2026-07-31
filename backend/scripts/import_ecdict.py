#!/usr/bin/env python3
"""
从 ECDICT CSV 筛选导入查词层 dict_entry / dict_lemma。

数据源: https://github.com/skywind3000/ECDICT （ecdict.csv 基础版约 76 万行）

用法:
  python import_ecdict.py --csv data/ecdict.csv --mysql
  python import_ecdict.py --csv data/ecdict.csv --frq-max 20000 --limit 10000 --out-sql ../.../V26__seed_dict.sql

环境变量（--mysql）:
  MYSQL_HOST MYSQL_PORT MYSQL_USER MYSQL_PASSWORD MYSQL_DB
  默认 localhost:3306 / island / island123 / island
"""
from __future__ import annotations

import argparse
import csv
import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
DEFAULT_CSV = ROOT / "data" / "ecdict.csv"

EXAM_TAGS = {"cet4", "cet6", "zk", "gk", "toefl", "ielts", "gre", "ky"}


def esc(s: str) -> str:
    return s.replace("\\", "\\\\").replace("'", "''")


def brief_zh(translation: str) -> str:
    if not translation:
        return ""
    lines = [ln.strip() for ln in translation.replace("\\n", "\n").splitlines() if ln.strip()]
    # 去掉过长英文 definition 混排，保留中文行
    kept = []
    for ln in lines:
        if re.search(r"[\u4e00-\u9fff]", ln):
            kept.append(re.sub(r"^[a-z]+\.\s*", "", ln, flags=re.I))
        if len(kept) >= 4:
            break
    text = "；".join(kept) if kept else lines[0][:200]
    return text[:900]


def parse_exchange(exchange: str) -> list[tuple[str, str, str]]:
    """返回 (variant, lemma, relation)。lemma 由调用方补全。"""
    if not exchange:
        return []
    out: list[tuple[str, str, str]] = []
    for part in exchange.split("/"):
        part = part.strip()
        if not part or ":" not in part:
            continue
        rel, form = part.split(":", 1)
        form = form.strip().lower()
        if not form or not re.match(r"^[a-z][a-z'-]*$", form):
            continue
        out.append((form, "", rel.strip()))
    return out


def should_keep(row: dict, frq_max: int, include_exam: bool) -> bool:
    word = (row.get("word") or "").strip().lower()
    if not word or len(word) > 64:
        return False
    if not re.match(r"^[a-z][a-z'-]*$", word):
        return False
    zh = brief_zh(row.get("translation") or "")
    if not zh or not re.search(r"[\u4e00-\u9fff]", zh):
        return False

    tags = set((row.get("tag") or "").lower().split())
    if include_exam and tags & EXAM_TAGS:
        return True
    try:
        if int(row.get("oxford") or 0) == 1:
            return True
    except ValueError:
        pass

    frq = parse_int(row.get("frq"))
    bnc = parse_int(row.get("bnc"))
    if frq is not None and 0 < frq <= frq_max:
        return True
    if bnc is not None and 0 < bnc <= frq_max:
        return True
    return False


def parse_int(v) -> int | None:
    if v is None or v == "":
        return None
    try:
        n = int(v)
        return n if n > 0 else None
    except (TypeError, ValueError):
        return None


def load_from_sqlite(db_path: Path, frq_max: int, include_exam: bool, limit: int | None):
    import sqlite3

    conn = sqlite3.connect(str(db_path))
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()
    # stardict 表字段与 CSV 一致
    cur.execute("SELECT name FROM sqlite_master WHERE type='table'")
    tables = [r[0] for r in cur.fetchall()]
    table = "stardict" if "stardict" in tables else tables[0]
    cur.execute(f"SELECT * FROM {table}")
    entries: list[dict] = []
    lemmas: dict[str, tuple[str, str]] = {}
    for row in cur:
        raw = {k: row[k] for k in row.keys()}
        if not should_keep(raw, frq_max, include_exam):
            continue
        word = raw["word"].strip().lower()
        zh = brief_zh(raw.get("translation") or "")
        entries.append(
            {
                "word": word,
                "phonetic": (raw.get("phonetic") or "")[:120],
                "meaning_zh": zh,
                "pos": (raw.get("pos") or "")[:60],
                "tags": (raw.get("tag") or "")[:120],
                "frq": parse_int(raw.get("frq")),
                "bnc": parse_int(raw.get("bnc")),
            }
        )
        for variant, _lemma, rel in parse_exchange(raw.get("exchange") or ""):
            if rel == "0":
                lemmas[word] = (variant, "0")
            elif variant != word:
                lemmas.setdefault(variant, (word, rel))
        if limit and len(entries) >= limit:
            break
    conn.close()
    return entries, lemmas


def load_filtered(csv_path: Path, frq_max: int, include_exam: bool, limit: int | None):
    if csv_path.suffix.lower() in {".db", ".sqlite", ".sqlite3"}:
        return load_from_sqlite(csv_path, frq_max, include_exam, limit)

    entries: list[dict] = []
    lemmas: dict[str, tuple[str, str]] = {}

    with csv_path.open("r", encoding="utf-8", errors="ignore", newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            if not should_keep(row, frq_max, include_exam):
                continue
            word = row["word"].strip().lower()
            zh = brief_zh(row.get("translation") or "")
            entry = {
                "word": word,
                "phonetic": (row.get("phonetic") or "")[:120],
                "meaning_zh": zh,
                "pos": (row.get("pos") or "")[:60],
                "tags": (row.get("tag") or "")[:120],
                "frq": parse_int(row.get("frq")),
                "bnc": parse_int(row.get("bnc")),
            }
            entries.append(entry)

            for variant, _lemma, rel in parse_exchange(row.get("exchange") or ""):
                if rel == "0":
                    lemmas[word] = (variant, "0")
                elif variant != word:
                    lemmas.setdefault(variant, (word, rel))

            if limit and len(entries) >= limit:
                break

    return entries, lemmas


def emit_sql(entries: list[dict], lemmas: dict[str, tuple[str, str]]) -> str:
    lines = [
        "-- ECDICT 子集种子（import_ecdict.py 生成）",
        f"-- entries={len(entries)} lemmas={len(lemmas)}",
        "",
    ]
    for e in entries:
        frq = "NULL" if e["frq"] is None else str(e["frq"])
        bnc = "NULL" if e["bnc"] is None else str(e["bnc"])
        lines.append(
            "INSERT INTO dict_entry (word, phonetic, meaning_zh, pos, tags, frq, bnc, source) VALUES "
            f"('{esc(e['word'])}', '{esc(e['phonetic'])}', '{esc(e['meaning_zh'])}', "
            f"'{esc(e['pos'])}', '{esc(e['tags'])}', {frq}, {bnc}, 'ecdict') "
            "ON DUPLICATE KEY UPDATE phonetic=VALUES(phonetic), meaning_zh=VALUES(meaning_zh), "
            "pos=VALUES(pos), tags=VALUES(tags), frq=VALUES(frq), bnc=VALUES(bnc);"
        )
    for variant, (lemma, rel) in lemmas.items():
        lines.append(
            "INSERT INTO dict_lemma (variant, lemma, relation) VALUES "
            f"('{esc(variant)}', '{esc(lemma)}', '{esc(rel)}') "
            "ON DUPLICATE KEY UPDATE lemma=VALUES(lemma), relation=VALUES(relation);"
        )
    return "\n".join(lines) + "\n"


def mysql_upsert(entries: list[dict], lemmas: dict[str, tuple[str, str]]) -> None:
    try:
        import pymysql
    except ImportError:
        print("需要 pymysql: pip install pymysql", file=sys.stderr)
        sys.exit(1)

    conn = pymysql.connect(
        host=os.getenv("MYSQL_HOST", "localhost"),
        port=int(os.getenv("MYSQL_PORT", "3306")),
        user=os.getenv("MYSQL_USER", "island"),
        password=os.getenv("MYSQL_PASSWORD", "island123"),
        database=os.getenv("MYSQL_DB", "island"),
        charset="utf8mb4",
        autocommit=False,
    )
    try:
        with conn.cursor() as cur:
            sql_e = (
                "INSERT INTO dict_entry (word, phonetic, meaning_zh, pos, tags, frq, bnc, source) "
                "VALUES (%s,%s,%s,%s,%s,%s,%s,'ecdict') "
                "ON DUPLICATE KEY UPDATE phonetic=VALUES(phonetic), meaning_zh=VALUES(meaning_zh), "
                "pos=VALUES(pos), tags=VALUES(tags), frq=VALUES(frq), bnc=VALUES(bnc)"
            )
            batch = [
                (e["word"], e["phonetic"], e["meaning_zh"], e["pos"], e["tags"], e["frq"], e["bnc"])
                for e in entries
            ]
            for i in range(0, len(batch), 500):
                cur.executemany(sql_e, batch[i : i + 500])
                print(f"  dict_entry {min(i + 500, len(batch))}/{len(batch)}")

            sql_l = (
                "INSERT INTO dict_lemma (variant, lemma, relation) VALUES (%s,%s,%s) "
                "ON DUPLICATE KEY UPDATE lemma=VALUES(lemma), relation=VALUES(relation)"
            )
            lbatch = [(v, lem, rel) for v, (lem, rel) in lemmas.items()]
            for i in range(0, len(lbatch), 1000):
                cur.executemany(sql_l, lbatch[i : i + 1000])
                print(f"  dict_lemma {min(i + 1000, len(lbatch))}/{len(lbatch)}")
        conn.commit()
        print(f"OK: entries={len(entries)} lemmas={len(lemmas)}")
    finally:
        conn.close()


def main() -> None:
    ap = argparse.ArgumentParser(description="Import ECDICT subset into dict_entry")
    ap.add_argument("--csv", type=Path, default=DEFAULT_CSV, help="ecdict.csv 或 .db/.sqlite")
    ap.add_argument("--db", type=Path, default=None, help="优先使用 SQLite（推荐 release ecdict-sqlite）")
    ap.add_argument("--frq-max", type=int, default=30000)
    ap.add_argument("--include-exam-tags", action="store_true", default=True)
    ap.add_argument("--no-exam-tags", action="store_true")
    ap.add_argument("--limit", type=int, default=None)
    ap.add_argument("--mysql", action="store_true", help="直接写入 MySQL")
    ap.add_argument("--out-sql", type=Path, default=None)
    args = ap.parse_args()

    src = args.db or args.csv
    if not src.exists():
        print(
            f"找不到 {src}\n"
            "推荐下载: https://github.com/skywind3000/ECDICT/releases/download/1.0.28/ecdict-sqlite-28.zip\n"
            "解压后: python import_ecdict.py --db data/stardict.db --mysql",
            file=sys.stderr,
        )
        sys.exit(1)

    include_exam = not args.no_exam_tags
    print(f"读取 {src} …")
    entries, lemmas = load_filtered(src, args.frq_max, include_exam, args.limit)
    print(f"筛选后 entries={len(entries)} lemmas={len(lemmas)}")

    if args.mysql:
        mysql_upsert(entries, lemmas)
    if args.out_sql:
        args.out_sql.parent.mkdir(parents=True, exist_ok=True)
        args.out_sql.write_text(emit_sql(entries, lemmas), encoding="utf-8")
        print(f"已写 SQL: {args.out_sql}")
    if not args.mysql and not args.out_sql:
        print("请指定 --mysql 或 --out-sql", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
