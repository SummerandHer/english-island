#!/usr/bin/env python3
"""为 dict_entry 中已有词条批量生成简易 lemma。"""
from __future__ import annotations

import os
import re
import pymysql

def ok(w: str) -> bool:
    return bool(re.match(r"^[a-z][a-z'-]*$", w)) and len(w) <= 64


def pairs_for(word: str) -> list[tuple[str, str, str]]:
    if len(word) < 3:
        return []
    out: list[tuple[str, str, str]] = []
    if word.endswith("y") and len(word) > 3 and word[-2] not in "aeiou":
        out.append((word[:-1] + "ies", word, "s"))
    elif word.endswith(("s", "x", "z")) or word.endswith(("ch", "sh")):
        out.append((word + "es", word, "s"))
    else:
        out.append((word + "s", word, "s"))
    if word.endswith("e") and len(word) > 3:
        out.append((word[:-1] + "ing", word, "i"))
        out.append((word + "d", word, "d"))
    else:
        out.append((word + "ing", word, "i"))
        out.append((word + "ed", word, "d"))
    return [(v, l, r) for v, l, r in out if v != l and ok(v)]


def main() -> None:
    conn = pymysql.connect(
        host=os.getenv("MYSQL_HOST", "localhost"),
        user=os.getenv("MYSQL_USER", "root"),
        password=os.getenv("MYSQL_PASSWORD", "12345678"),
        database=os.getenv("MYSQL_DB", "island"),
        charset="utf8mb4",
    )
    cur = conn.cursor()
    cur.execute("SELECT word FROM dict_entry")
    words = [r[0] for r in cur.fetchall()]
    lemmas: dict[str, tuple[str, str]] = {}
    for w in words:
        for v, l, r in pairs_for(w):
            lemmas.setdefault(v, (l, r))
    sql = (
        "INSERT INTO dict_lemma (variant, lemma, relation) VALUES (%s,%s,%s) "
        "ON DUPLICATE KEY UPDATE lemma=VALUES(lemma)"
    )
    batch = [(v, lem, rel) for v, (lem, rel) in lemmas.items()]
    for i in range(0, len(batch), 1000):
        cur.executemany(sql, batch[i : i + 1000])
    conn.commit()
    print(f"words={len(words)} lemmas_upserted={len(batch)}")
    conn.close()


if __name__ == "__main__":
    main()
