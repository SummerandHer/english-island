# 后端 Agent 须知

## 1. 技术栈（不可擅自替换 MVP 栈）

- Java **21** + **Spring Boot 3.x**
- **Maven** 构建
- **MyBatis-Plus 3.5** + MySQL 8、Redis
- **Flyway** 负责 DDL/种子数据；**禁止** JPA `ddl-auto=update`
- 鉴权：JWT；登录对接微信扫码（MVP 可用邮箱密码兜底）
- 文件：本地 `uploads/`，抽象 `FileStorageService` 便于迁 COS/OSS

## 2. 包结构建议

```
com.island
├── config          # Security, Redis, Cors, OSS 预留
├── module
│   ├── auth
│   ├── user
│   │   └── mapper/     # UserMapper extends BaseMapper
│   ├── post        # 社区 Feed
│   ├── reading
│   ├── translation # 含 AI 批改
│   ├── video
│   └── vip
├── common          # 统一响应、异常、枚举
└── IslandApplication.java
```

## 3. API 约定

- 前缀：`/api/v1`
- 统一响应：`{ "code": 0, "message": "ok", "data": {} }`
- 分页：`page`（从 1 开始）、`size`（默认 20）
- 错误码：业务错误用 4xx + 明确 message；未登录 401

## 4. 核心域模型要点

- 简单 CRUD：`LambdaQueryWrapper` + `BaseMapper`
- 复杂 SQL：放 `resources/mapper/**/*.xml` 或 `@Select` 注解
- 分页：`Page<T>` + `PaginationInnerInterceptor`（已配置）

### 用户 VIP

- `vip_level = 0` 普通；`vip_level = 1` 且 `vip_expire_at > NOW()` 为 VIP
- 权限校验逻辑见 `User.isVipActive()`

### 社区帖子

- 仅支持：**纯文字**或**文字+多图**
- 禁止：视频附件、外链字段（MVP 不实现）
- 图片走上传接口，存相对路径或 OSS key

### 阅读 / 翻译章节

- 章节：`title`, `sort_order`, `content_html`, `is_vip`（高级技巧）
- 阅读练习（**Phase 2 P0**，表已建、API 待做）：`reading_passage` → `reading_question` + `reading_question_option`；提交可写 `user_reading_progress` 或独立 submission 表（优先复用客观题即时判分，不落 LLM）
- 翻译题：`prompt_zh` / `prompt_en`, `reference_answer`, `is_mock`（模拟题标记）
- Phase 2 任务清单：[04-阅读翻译Phase2规划.md](./04-阅读翻译Phase2规划.md)

### 翻译 AI 批改（MVP 必做接口）

```
POST /api/v1/translation/submissions
Body: { "questionId": 1, "userAnswer": "..." }
Response: {
  "score": 85,
  "errors": [{ "span": "...", "suggestion": "...", "reason": "..." }],
  "referenceHint": "...",
  "overallComment": "..."
}
```

- 实现：`TranslationGradingService.gradeWithLlm`（四六级维度 Prompt）+ `fallbackGrade`；结果落库 `translation_submission`
- Phase 2 增强：响应增加 `cetScore`（0–15）、`band`（档位）；`GET /translation/submissions/mine` 历史
- 配置：`island.ai.enabled`、`island.ai.api-key`（勿提交密钥到 Git）

### 视频（含 4K 扩展预留）

```sql
video (
  id, title, cover_url,
  storage_type  ENUM('embed','oss') DEFAULT 'embed',  -- MVP 用 embed
  source_url,          -- 原始页 URL
  embed_code,          -- iframe 或播放器 ID
  provider,            -- bilibili / youtube / self
  is_vip,
  duration_sec
)

video_sentence (
  id, video_id, seq,
  start_ms, end_ms,
  text_en, text_zh
)

video_favorite (user_id, video_id)
```

- **MVP 不存视频二进制**；只存元数据 + 句级字幕时间轴
- 后期 OSS：`storage_type=oss`, `source_url` 改为 CDN URL

### 合规

- 题目 `is_mock = true`，`content_source` 记录「自编模拟」
- 视频记录来源 URL 与授权说明字段 `license_note`

## 5. 缓存策略

| Key 模式 | 内容 | TTL |
|----------|------|-----|
| `chapter:reading:{id}` | 章节 HTML | 1h |
| `video:meta:{id}` | 视频+句子列表 | 30m |
| `feed:posts:page:{n}` | Feed 分页 | 5m |

- 不缓存视频文件流

## 6. 安全

- 上传：限制 MIME（jpeg/png/webp）、单张 ≤ 5MB
- VIP 接口：`@PreAuthorize` 或拦截器校验 `vip_level` 与过期时间
- CORS：仅允许前端域名

## 7. MVP 交付清单（后端）

- [ ] 脚手架 + Flyway 初始迁移
- [ ] 注册/登录/JWT
- [ ] 阅读章节 CRUD（只读 API + 种子数据）
- [ ] 翻译章节 + 1 题 + AI 批改
- [ ] 视频列表/详情/句子轴/收藏
- [ ] 帖子发帖（文字+图）+ Feed
- [ ] VIP 权限拦截骨架
- [ ] `application-dev.yml.example`（无真实密钥）

## 8. 禁止事项

- ❌ 爬取四六级真题批量入库
- ❌ 下载 YouTube/B 站视频到服务器
- ❌ 在代码库提交 API Key、微信 Secret
- ❌ MVP 引入 Spring Cloud（单体即可）
