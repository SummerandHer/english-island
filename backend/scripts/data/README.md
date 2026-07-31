# 词库 / 词典数据目录

> 本目录文件**仅用于开发/运维导入脚本**，Java 运行时不读取。大文件已 gitignore，导入完成后可删本地副本。

| 文件 | Git | 用途 |
|------|-----|------|
| `cet4_core.json` | 跟踪 | 无大 JSON 时的词书兜底 |
| `CET4_1.json` / `CET6_1.json` | 忽略 | KyleBing 词书（`import_vocabulary.py` / `seed_dict_from_kylebing.py`） |
| `cet4_supplement.json` | 忽略 | 补充词 |
| `ecdict.csv` | 忽略 | CSV 导入备选（可能缺头词） |
| `ecdict-sqlite-28.zip` / `stardict.db` | 忽略 | **推荐** 完整 ECDICT 查词源 |

勿再提交 `CET4.json` / `CET6.json`（与 `_1` 副本重复）。

---

## 推荐：ECDICT SQLite（完整）

```powershell
cd backend\scripts\data
# 任选一源（约 207MB）
curl.exe -L -o ecdict-sqlite-28.zip "https://github.com/skywind3000/ECDICT/releases/download/1.0.28/ecdict-sqlite-28.zip"
# 或镜像：https://ghfast.top/https://github.com/skywind3000/ECDICT/releases/download/1.0.28/ecdict-sqlite-28.zip
Expand-Archive ecdict-sqlite-28.zip -DestinationPath .
cd ..
pip install pymysql
$env:MYSQL_USER='root'; $env:MYSQL_PASSWORD='你的密码'
python import_ecdict.py --db data\stardict.db --frq-max 50000 --mysql
```

导入成功后可删除本地 `stardict.db` / zip / csv（需时按上文重下）。

---

## 本地已跑的兜底（不依赖大文件）

```powershell
$env:MYSQL_USER='root'; $env:MYSQL_PASSWORD='你的密码'
python seed_dict_core_bootstrap.py   # ~800 外刊高频
python seed_dict_from_kylebing.py --mysql
python expand_dict_lemmas.py
```

---

## Flyway 词汇种子约定（重要）

- **已发布** `V9` / `V11` / `V13` / `V17` 为历史快照，**禁止修改或合并**（checksum）。
- 今后扩词：**只新增增量** migration（`V26+`），用 `INSERT ... ON DUPLICATE KEY UPDATE` 或仅插新词；**禁止**再生成覆盖全库的巨型 SQL。
- 词书 regenerate 用本目录 JSON + 脚本，不要把全量 dump 写进 Git。

合规：开源整理，仅供学习；禁止声称官方释义。
