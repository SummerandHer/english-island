# AGENT.md — ISLAND（四六级岛）编程智能体入口

> **读者**：编程智能体（非人类用户）。编码前读完本文件 + [00-总览.md](./00-总览.md) 索引的全部规范。
>
> **说明**：用户指定的参考路径 `english(源码未改动版)/flygpt` 在本机不存在；本文档基于当前工作区 `english/` 源码编写，覆盖 ISLAND 单体仓库全貌。前后端细则分别见 [01-后端Agent须知.md](./01-后端Agent须知.md)、[02-前端Agent须知.md](./02-前端Agent须知.md)、[03-双语模块UI规范.md](./03-双语模块UI规范.md)——**勿在本文件重复展开**。

---

## 1. 项目是什么

**ISLAND（四六级岛）**：面向中国大陆大学生的 **CET-4/6 垂直学习 Web 平台**（MVP 已交付；当前 **v0.3 Phase 2.5**：阅读模拟 + 词汇每日 20 词可测，详见 [06-迭代路线图.md](./06-迭代路线图.md)）。

| 模块 | 用户路径 | 后端域 |
|------|----------|--------|
| 首页 | `/` | — |
| 阅读技巧 + 模拟练习 | `/reading`、`/reading/practice/[id]` | `module/reading`（含 `ReadingPassageService`） |
| 翻译技巧 + AI 批改 | `/translation`、`/translation/practice/[id]` | `module/translation` |
| 每日词汇 | `/vocabulary` | `module/vocabulary` |
| **日报岛** | `/daily`、`/daily/:id` | `module/daily`（见 [08-日报岛规划.md](./08-日报岛规划.md)） |
| **仿真题岛** | `/islands/exam`（短篇/长篇/练习）；旧路径 `/sim-exam` 重定向 | `module/simexam`（见 [09](./09-仿真题岛规划.md)/[10](./10-v1.5仿真题岛P0开发任务.md)）；P0+P0.5 已落地 |
| 双语视频精听 | `/video` | `module/video` |
| 社区 Feed | `/feed` | `module/post` |
| 管理后台（视频入库） | `/admin/**` | `module/video/admin`、`module/admin`（含日报） |

**产品边界（硬约束）**：只做英语四六级；Web + 平板响应式；MVP 不爬真题、不转存第三方视频；社区仅文字+图；VIP 字段已预留支付二期。

---

## 2. 仓库布局

```
english/
├── backend/          # Spring Boot 3 单体 API (:8080)
├── frontend/         # Nuxt 3 SPA/SSR (:3000)
├── uploads/          # local 存储时的文件根（storage-type=local）
├── Agent须知/        # 本目录：智能体规范 + 本文件
├── docker-compose.yml # MySQL 8 + Redis 7
├── 项目规划.md        # 产品/架构决策（人类可读，Agent 需知 MVP 范围）
├── 库表设计.sql       # 完整 DDL（与 Flyway V1~V4 对齐）
└── README.md          # 启动命令与 API 速查
```

---

## 3. 技术栈（勿擅自替换 MVP 选型）

| 层 | 选型 |
|----|------|
| 后端 | Java 21, Spring Boot 3.3, Maven, MyBatis-Plus 3.5, Flyway, JWT |
| 数据 | MySQL 8, Redis 7（验证码；可降级 memory） |
| 前端 | Nuxt 3, Vue 3, TypeScript, Tailwind, Naive UI, Pinia |
| 文件 | `FileStorageService` 抽象 → `local` 或阿里云 OSS |
| AI | DeepSeek（翻译批改占位 + 字幕中译草稿） |
| ASR | faster-whisper Python 子进程（Admin 视频解析） |

---

## 4. 系统架构

```
┌──────────────┐  REST/JSON + Bearer JWT   ┌─────────────────────────┐
│ Nuxt 3 :3000 │ ◄──────────────────────► │ Spring Boot 单体 :8080   │
│ useApi()     │                          │ com.island.module.*     │
└──────────────┘                          └───────────┬─────────────┘
                                                      │
                    ┌─────────────────────────────────┼──────────────────┐
                    ▼                                 ▼                  ▼
                 MySQL                           Redis              OSS / uploads/
              (Flyway V1~4)                  (验证码)            (file_asset 元数据)
```

**跨层约定**

- API 前缀 `/api/v1`；响应 `{ "code": 0, "message": "ok", "data": T }`（`ApiResponse`）
- 分页 `{ items, total, page, size }`（`PageResult`）
- 业务异常 `BusinessException` → `GlobalExceptionHandler` 映射 HTTP 状态
- 鉴权头 `Authorization: Bearer <jwt>`；JWT subject = `userId`

---

## 5. 后端：包结构与模块地图

根包 `com.island`：

```
config/          SecurityConfig, IslandProperties, MybatisPlusConfig, OssConfig, RedisConfig
security/        JwtAuthFilter, JwtTokenProvider, IslandUserDetails(+Service)
storage/         FileStorageService → Local / Oss 实现
common/          ApiResponse, PageResult, BusinessException, GlobalExceptionHandler
module/
  auth/          注册/登录/验证码/重置密码 → AuthController
  user/          User 实体（vipLevel, role, isVipActive(), isAdmin()）
  file/          通用文件 CRUD
  post/          社区 Feed + 图片上传
  reading/       阅读章节 + 模拟篇章/判分（ReadingPassageService）
  vocabulary/    每日 20 词、查词、生词本、复习队列
  daily/         日报岛精读、打卡、标注、AI 解析（见 08）
  simexam/       仿真题岛：底稿、仿写生成、短篇练习/判分（见 09/10）
  translation/     章节 + 题目 + TranslationGradingService（DeepSeek 批改，未配置 API 时 fallback）
  video/         用户端视频列表/详情/收藏
  video/admin/   Admin 视频流水线（parse → publish → 句轴编辑）
  video/asr/     WhisperAsrService
  video/ai/      SentenceZhDraftService
  admin/         AdminContentController（翻译/阅读/日报录题）
```

**MyBatis-Plus**：全部 `*Mapper extends BaseMapper<T>`，无 XML；`@MapperScan("com.island.module.**.mapper")`；`IslandMetaObjectHandler` 自动填 `createdAt/updatedAt`。

---

## 6. 鉴权与权限（Agent 必记）

### 6.1 Spring Security 路径规则（`SecurityConfig`）

| 路径 | 规则 |
|------|------|
| `/api/v1/auth/**` | 公开 |
| `GET /reading/**`, `GET /translation/chapters/**`, `GET /daily/topics|hub|articles/*`, `GET /videos/**`, `GET /posts/**`, `GET /files/**` | 公开 |
| `/uploads/**` | 公开（local 静态） |
| `/api/v1/admin/**` | `ROLE_ADMIN` |
| 其余 | 需登录 |

### 6.2 角色来源（`IslandUserDetails`）

- `ROLE_USER`：所有用户
- `ROLE_ADMIN`：`user.role == "ADMIN"`（V4 迁移：首个注册用户自动 ADMIN）
- `ROLE_VIP`：`user.isVipActive()`（`vipLevel>=1` 且 `vipExpireAt > now`）

### 6.3 业务层 VIP 门控（非 Security 过滤器）

- `ReadingService` / `TranslationService`：`isVip=1` 章节需登录且 VIP，否则 403
- `VideoService.getVideo`：**当前对所有 VIP 视频直接 403**（与阅读/翻译行为不一致——改 VIP 视频逻辑时注意对齐）

### 6.4 注册约束

- 仅 **QQ 邮箱**（`QqEmailValidator`）
- 流程：`send-code` → `register`（验证码 Redis/memory，邮件 QQ SMTP）

---

## 7. Admin 视频流水线（核心增量能力）

```
上传 MP4 → POST /admin/videos/parse
         → FFmpeg 抽音 + whisper_transcribe.py (faster-whisper)
         → 可选 generateZh=true → SentenceZhDraftService (DeepSeek)
         → 视频上传 OSS/local → 返回 playUrl + sentences[] + videoObjectKey
         → 人工校对句轴
         → POST /admin/videos 发布（写 video + video_sentence）
```

| 步骤 | 端点 | 说明 |
|------|------|------|
| 解析 | `POST /admin/videos/parse` | multipart，`uploadForm` timeout 20min |
| 封面 | `POST /admin/files/cover` | |
| 发布 | `POST /admin/videos` | 最少句数 `island.admin.min-sentences-on-publish` |
| 编辑 | `PUT /admin/videos/{id}`, `PUT .../sentences` | |
| 中译草稿 | `POST /admin/videos/{id}/zh-draft` | |
| 系列 | `GET/POST /admin/video-series`, `PUT .../{id}` | |

前端对应页：`frontend/pages/admin/videos/new.vue`（新建）、`[id].vue`（编辑）、`middleware/admin.ts` 守卫。

---

## 8. 前端：路由与集成模式

### 8.1 关键配置

- `NUXT_PUBLIC_API_BASE` → `runtimeConfig.public.apiBase`（默认 `http://localhost:8080`）
- 主题色 `#18a058`（`assets/css/main.css` `--island-primary`）

### 8.2 数据访问（统一入口）

```typescript
const { request, uploadForm } = useApi()  // composables/useApi.ts
// 自动带 Bearer；code!==0 抛 Error；类型见 types/api.ts
```

### 8.3 认证状态

- Store：`stores/auth.ts` → localStorage `island_token` / `island_user`
- `isAdmin`：`user.admin || user.role === 'ADMIN'`
- **无全局 auth middleware**（除 admin）；各页 `onMounted` 调 `auth.hydrate()`
- Auth/Admin 页：`ssr: false`

### 8.4 双语播放页已知限制

`pages/video/[id].vue`：B 站 iframe **无法控制播放**；MVP 用 `setInterval` 模拟 `simulatedMs` 驱动 `useVideoSync`。换 native `<video>` 或 OSS 源时复用同一 `VideoSentence` 数据结构。UI 布局见 [03-双语模块UI规范.md](./03-双语模块UI规范.md)。

---

## 9. 数据模型（已实现 vs 仅 DDL）

### 9.1 有 Java 模块的表

`user`, `file_asset`, `post`, `post_image`, `reading_chapter`, `translation_chapter`, `translation_question`, `translation_submission`, `video`, `video_series`, `video_sentence`, `video_favorite`

### 9.2 V1 已建表、尚无 Java 实现（勿误加 API）

`user_wechat`, `vip_plan`, `vip_order`, `reading_passage`, `reading_question`, `reading_question_option`, `vocabulary`, `user_vocabulary`, `user_vocab_review`, `post_like`, `post_comment`, `user_video_progress`, `content_source`, `admin_audit_log`, `sentence_pattern`

### 9.3 Flyway

| 版本 | 内容 |
|------|------|
| V1 | 全量 schema |
| V2 | 种子：2 阅读章、2 翻译章、1 翻译题、3 embed 视频+句轴 |
| V3 | `user.email_verified` |
| V4 | `user.role`；首用户 ADMIN |
| V7 | 阅读模拟：2 篇章×4 题 |
| V8 | 词汇字段扩展 + `user_vocab_review` |
| V9 | 词汇核心种子（约 70 词） |
| V10 | 翻译题扩至 8 道 |
| V11 | 词汇 upsert 扩量（~205 词） |
| V12 | 阅读模拟 +2 篇 |
| V13–V14 | 词汇 800 + 章节推荐视频 |
| V15 | 阅读提交历史 `reading_submission` |
| V16–V19 | 阅读扩篇、词汇 1417、翻译 14 题（v1.3） |

DDL 权威参考：`库表设计.sql`。**禁止** JPA `ddl-auto=update`。

---

## 10. 配置密钥（`island.*` + 环境）

| 键 | 用途 |
|----|------|
| `island.jwt.secret` | JWT（生产必改） |
| `island.upload.storage-type` | `local` \| `oss` |
| `island.oss.*` | OSS 凭证与 bucket |
| `island.ai.*` | DeepSeek：`enabled`, `api-key`, `base-url` |
| `island.whisper.*` | python 路径、model、device、script-path |
| `island.ffmpeg.path` | ASR 抽音频 |
| `island.verification.store` | `redis` \| `memory` |
| `spring.mail.*` | QQ SMTP（`application-dev.yml`，勿提交 Git） |

本地：复制 `backend/src/main/resources/application-dev.yml.example` → `application-dev.yml`。

---

## 11. 启动命令

```bash
# 依赖
docker compose up -d                    # MySQL :3306 island/island123, Redis :6379

# 后端
cd backend && .\mvnw.cmd spring-boot:run   # Windows
# API http://localhost:8080

# 前端
cd frontend && npm install && npm run dev
# Web http://localhost:3000
```

---

## 12. 常见任务 → 改哪里

| 任务 | 位置 |
|------|------|
| 新增 REST 模块 | `backend/.../module/<name>/` + Flyway 新迁移 |
| 改 API 权限 | `SecurityConfig` + Service 层 VIP 校验 |
| 新增用户页 | `frontend/pages/` + `useApi` + 可选 `types/api.ts` |
| 双语播放器/句轴 | `useVideoSync.ts`, `components/video/*`, `pages/video/[id].vue` |
| Admin 视频流程 | `AdminVideoService`, `pages/admin/videos/*` |
| 翻译 AI 批改 | `TranslationGradingService`（`gradeWithLlm` + `fallbackGrade`） |
| 阅读模拟题 Phase2 | `reading_passage/question` 表已有 → 见 [04-阅读翻译Phase2规划.md](./04-阅读翻译Phase2规划.md) |
| 文件存储切换 | `island.upload.storage-type`, `OssFileStorageService` |
| UI 视觉/布局 | 遵循 02/03 规范 + 参考图 |

---

## 13. 已知缺口与陷阱（避免重复踩坑）

### 13.1 产品能力缺口（Phase 2 优先）

| 模块 | 缺口 | 规划 |
|------|------|------|
| 阅读 | ~~仅章节 HTML~~；模拟题 API + 练习页 + V7 种子 | P0-1 已完成 |
| 词汇 | 每日 20 词 + 生词本 + 打卡；**1417 词**（V17 upsert） | ✅ v1.3 |
| Admin 内容 | 翻译/阅读 CRUD `/admin/content/**` | ✅ v1.0 |
| 阅读进度 | `POST .../chapters/{slug}/progress` | ✅ v1.0 |
| 翻译 | **14 道**模拟题（V10+V18）、练习/历史/15 分制 | ✅ |
| 备考导航 | `/exam-guide` | ✅ |
| 词汇 1417 | V17 `import_vocabulary.py` | ✅ v1.3 |
| 双语 | VIP 视频逻辑不一致；Embed 无法真同步；新视频走 OSS | P0-6 + 运营上传 |

### 13.2 技术债务

1. **TranslationGradingService**：已实现 DeepSeek；`island.ai.api-key` 未配置时走 `fallbackGrade`（前端应展示「占位评分」标签，已实现）
2. **VIP 视频**：`VideoService` 对 `isVip=1` 一律 403，未查用户 VIP 状态
3. **B 站 Embed**：无法 seek/倍速/暂停联动句轴；`useVideoSync` 模拟时钟仅为 MVP 演示
4. **无 Token 刷新**；`reading/[slug].vue` 等仍用 `alert()` 报错
5. **Admin 系列页**：仅创建+列表，无编辑/删除 UI；**无阅读/翻译题目 Admin**
6. **multipart/连接超时** 20 分钟——大视频 ASR 专用，勿随意缩短
7. **合规**：不爬真题批量入库、不下载转存 B 站/YouTube 视频到服务器

---

## 14. 禁止事项（Agent 硬规则）

- ❌ 替换 Nuxt/React 或 Spring Cloud 微服务化（MVP 单体）
- ❌ JPA/Hibernate 自动建表
- ❌ 在仓库提交 API Key、OSS Secret、邮件授权码
- ❌ 社区帖支持视频/外链（产品约束）
- ❌ 偏离 [03-双语模块UI规范.md](./03-双语模块UI规范.md) 布局（除非用户明确要求）
- ❌ 未经确认擅自做 P2 词汇本/支付（见 [04-阅读翻译Phase2规划.md](./04-阅读翻译Phase2规划.md)）
- ❌ MVP 范围外大功能（见 [项目规划.md](../项目规划.md) 第二期列表）
- ❌ 仿真题岛：对用户露出真题底稿；第三方教辅作底稿；AI 失败时用手工改写冒充仿真卷发布；P0 提交前开放点词/全文译（见 [09](./09-仿真题岛规划.md)/[10](./10-v1.5仿真题岛P0开发任务.md)）

---

## 16. 竞品定位与差异化（ISLAND 应打什么牌）

| 赛道 | 代表产品 | 强项 | ISLAND 不做 | ISLAND 做 |
|------|----------|------|-------------|-----------|
| 真题/词书 | 扇贝、金山词霸、星火 | 真题卷、背单词、名师解析 | 批量真题爬取 | **自编模拟题** + 技巧章节 |
| AI 泛读 | AI Level English | 分级改写、逐句精读、闪卡 | 泛 CEFR 非考试 | **四六级题型**技巧 + 模拟练 |
| AI 写作 | Ries、ChatGPT | 中式英语、通用批改 | 通用插件 | **汉译英定档扣分**（对齐大纲） |
| 视频 | Ries、各平台 | 双语字幕 | 转存第三方视频 | **句轴精听** + 技巧联动 |

**突破点（Phase 2）**：「章节方法论 → 同主题模拟题 → AI 分档批改 → 历史弱项 → 推荐双语精听」一条链；详见 [04-阅读翻译Phase2规划.md](./04-阅读翻译Phase2规划.md)。

---

## 17. Phase 2 工作流（Agent 必遵）

```
需求/分析 → 读 06 路线图确认版本与项 → 04/05 细则 → Flyway/API → 前端 → 更新 06 + §13
```

| 版本 | 范围 | 状态 |
|------|------|------|
| **v0.3** | 阅读模拟、词汇 v0、翻译闭环 | ✅ |
| **v0.4** | 8 翻译题、备考导航、VIP 视频、今日任务 | ✅ |
| **v1.0 运营向 A** | Admin 录题、阅读进度、生词本、V11–V14 | ✅ |
| **v1.2 备考闭环** | 阅读历史、点词生词、词汇 Tab、移动导航、P2 汇总/限时/缓存 | ✅；见 [07](./07-v1.2备考闭环P0开发任务.md) |
| **v1.4 日报岛 P0** | 周一/三/五精读打卡、标注、Admin AI 解析 | 🚧 **当前主线**；见 [08-日报岛规划.md](./08-日报岛规划.md) |
| **v1.5 仿真题岛** | 真题底稿→高仿长短篇阅读、先考后学、考点解析 | 🚧 P0+P0.5 已编码（V22–V23）；见 [10](./10-v1.5仿真题岛P0开发任务.md) |
| **v1.1 变现向** | 支付、VIP 订单 | 📋 未选，暂缓 |

**产品决策（已确认）**：词汇 **1B**；真题 **2A**；上线 **3A**；禁止 RACE 商用入库；新视频 **OSS**。

动功能代码前：**必须先读** [06-迭代路线图.md](./06-迭代路线图.md)；日报岛读 [08-日报岛规划.md](./08-日报岛规划.md)；仿真题岛读 [09](./09-仿真题岛规划.md)+[10](./10-v1.5仿真题岛P0开发任务.md)；v1.2 任务读 [07](./07-v1.2备考闭环P0开发任务.md)；阅读/翻译细则见 [04](./04-阅读翻译Phase2规划.md)；数据源见 [05](./05-内容与数据源规划.md)。

---

## 18. 阅读顺序（新会话 Agent）

1. **本文件**（全局架构）
2. [00-总览.md](./00-总览.md) → 按任务读 01 / 02 / 03
3. **做阅读/翻译功能** → [04-阅读翻译Phase2规划.md](./04-阅读翻译Phase2规划.md)
4. **排期与待办** → [06-迭代路线图.md](./06-迭代路线图.md)（**首选**）；日报岛 → [08](./08-日报岛规划.md)；仿真题岛 → [09](./09-仿真题岛规划.md)+[10](./10-v1.5仿真题岛P0开发任务.md)；v1.2 → [07](./07-v1.2备考闭环P0开发任务.md)
5. **词汇/内容/合规** → [05-内容与数据源规划.md](./05-内容与数据源规划.md)
6. 动数据库前读 `库表设计.sql` 或 `backend/.../db/migration/`（当前 **V20**）
7. 动双语 UI 前打开 `双语页面视频的展示参考模版.png`
8. 不确定产品边界时读 [项目规划.md](../项目规划.md)

---

*最后同步：Flyway V23（仿真题岛长篇匹配）；日报岛见 08；仿真题见 09/10；路线图见 06。*
