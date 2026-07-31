# ISLAND · 四六级岛

面向中国大陆大学生的 **英语四六级垂直学习平台**。提供阅读/翻译技巧学习、双语视频精听、社区交流与 AI 翻译批改。

---

## 产品模块

| 模块 | 路径 | 说明 |
|------|------|------|
| 首页 | `/` | 四大模块入口 |
| 阅读技巧 | `/reading` | 章节式方法论（书籍形态） |
| 翻译技巧 | `/translation` | 章节 + 模拟题 + **AI 批改** |
| 双语视频 | `/video` | B 站 Embed + 句级字幕同步 |
| 全平台 | `/feed` | 社区 Feed（仅文字 + 图片） |

**VIP 权益（字段已预留）**：高级技巧章节、AI 批改、专属视频。

---

## 技术栈

### 后端 `backend/`

| 技术 | 版本/说明 |
|------|-----------|
| Java | 21 |
| Spring Boot | 3.3.x |
| 构建 | Maven |
| ORM | **MyBatis-Plus 3.5** |
| 数据库 | MySQL 8 + Flyway 迁移 |
| 缓存 | Redis 7 |
| 鉴权 | JWT（邮箱密码登录，微信扫码二期） |
| AI | 翻译批改（DeepSeek 等，可配置） |

### 前端 `frontend/`

| 技术 | 版本/说明 |
|------|-----------|
| Nuxt | 3.x |
| Vue | 3 + TypeScript |
| UI | Tailwind CSS + Naive UI |
| 状态 | Pinia |
| 风格 | 清新校园风 |

---

## 架构概览

```
┌─────────────┐     REST/JSON      ┌──────────────────┐
│  Nuxt 3 Web │ ◄────────────────► │ Spring Boot 单体  │
│  :3000      │                    │  :8080            │
└─────────────┘                    └────────┬─────────┘
                                            │
                              ┌─────────────┼─────────────┐
                              ▼             ▼             ▼
                           MySQL         Redis      uploads/
                         (Flyway)                    (→ OSS)
```

- **视频**：MVP 使用 B 站官方 Embed，站内维护句级字幕轴；表结构预留 `storage_type=oss` 支持后期 4K 自建库。
- **题目**：MVP 以自编模拟题为主，不批量使用真题，规避版权风险。

---

## 目录结构

```
english/
├── backend/                 # Spring Boot API
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/      # Flyway（V1 建表 + V2 种子数据）
├── frontend/                # Nuxt 3 前端
├── uploads/                 # 本地图片上传（社区帖）
├── Agent须知/               # 双 Agent 开发规范 + UI 参考图
├── docker-compose.yml       # MySQL + Redis
├── 项目规划.md              # 产品与技术规划
├── 库表设计.md / .sql       # 表域索引；DDL 以 Flyway 为准
├── Agent须知/UI参考/        # 双语 UI 图 + 视频源推荐.md
└── README.md
```

---

## 环境要求

- **JDK 21**
- **Node.js 18+**（推荐 20+）
- **MySQL 8** + **Redis 7**
- **Docker Desktop**（可选，推荐）

---

## 快速启动

### 1. 启动数据库

**Docker（推荐）**

```bash
docker compose up -d
```

默认配置：

| 服务 | 地址 | 账号 | 密码 |
|------|------|------|------|
| MySQL | `localhost:3306` | `island` | `island123` |
| Redis | `localhost:6379` | — | — |

数据库名：`island`（首次启动自动创建）

**无 Docker 时**，请本地创建数据库并修改 `backend/src/main/resources/application.yml` 中的连接信息。

### 2. 启动后端

```bash
cd backend

# Windows / Linux / macOS（无需全局安装 Maven）
./mvnw spring-boot:run          # Linux/macOS
.\mvnw.cmd spring-boot:run    # Windows

# 打包
./mvnw clean package -DskipTests
java -jar target/island-backend-0.1.0-SNAPSHOT.jar
```

- API 基址：`http://localhost:8080`
- **Flyway 管表结构，MyBatis-Plus 管读写**；不使用 JPA/Hibernate 自动建表。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

- 访问：`http://localhost:3000`
- API 代理配置：环境变量 `NUXT_PUBLIC_API_BASE`（默认 `http://localhost:8080`）

### 4. 生产构建

```bash
# 前端
cd frontend && npm run build && npm run preview

# 后端
cd backend && mvn clean package -DskipTests
java -jar target/island-backend-0.1.0-SNAPSHOT.jar
```

---

## 配置说明

### 后端 `application.yml`

| 配置项 | 说明 |
|--------|------|
| `spring.datasource.*` | MySQL 连接 |
| `mybatis-plus.*` | MyBatis-Plus 配置（驼峰映射、日志等） |
| `island.jwt.secret` | JWT 密钥（生产环境务必修改） |
| `island.upload.dir` | 上传目录，默认 `../uploads` |
| `island.cors.allowed-origins` | 前端域名，默认 `http://localhost:3000` |
| `island.ai.*` | AI 批改：`enabled`、`api-key`、`base-url` |

本地开发可复制 `application-dev.yml.example` 为 `application-dev.yml` 并填写密钥（该文件已加入 `.gitignore`）。

### 启用 AI 翻译批改

```yaml
island:
  ai:
    enabled: true
    provider: deepseek
    api-key: sk-your-key
    base-url: https://api.deepseek.com
```

未配置时，批改接口返回 **占位评分**，便于 MVP 联调。

---

## API 概览

统一前缀：`/api/v1`  
统一响应：`{ "code": 0, "message": "ok", "data": {} }`

| 模块 | 方法 | 路径 | 鉴权 |
|------|------|------|------|
| 注册 | POST | `/auth/register` | 否 |
| 登录 | POST | `/auth/login` | 否 |
| 当前用户 | GET | `/auth/me` | 是 |
| 阅读章节列表 | GET | `/reading/chapters` | 否 |
| 阅读章节详情 | GET | `/reading/chapters/{slug}` | 否（VIP 章需登录+VIP） |
| 翻译章节 | GET | `/translation/chapters` | 否 |
| 翻译题目 | GET | `/translation/questions` | 否 |
| 翻译批改 | POST | `/translation/submissions` | 是 |
| 视频列表（分页） | GET | `/videos?page=1&size=7` | 否 |
| 视频详情+句轴 | GET | `/videos/{id}` | 否（VIP 视频 403） |
| 收藏视频 | POST | `/videos/{id}/favorite` | 是 |
| Feed | GET | `/posts?page=1&size=20` | 否 |
| 发帖 | POST | `/posts` | 是 |
| 上传图片 | POST | `/posts/upload` | 是 |

---

## 测试账号

### 注册

访问 `http://localhost:3000/login`，或使用 API：

```bash
curl -X POST http://localhost:8080/api/v1/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"test@island.dev\",\"password\":\"123456\",\"nickname\":\"测试岛民\"}"
```

### 手动开通 VIP

```sql
UPDATE `user`
SET vip_level = 1, vip_expire_at = '2026-12-31 23:59:59'
WHERE email = 'test@island.dev';
```

---

## 种子数据

Flyway `V2__seed_data.sql` 已预置：

- 阅读章节 × 2
- 翻译章节 × 2（含 1 章 VIP）
- 翻译模拟题 × 1
- 双语视频 × 3（B 站 Embed，含 TED-Ed 示例句轴）

视频源详见 [Agent须知/UI参考/视频源推荐.md](./Agent须知/UI参考/视频源推荐.md)。

---

## 开发规范

本项目采用 **一人 + 双 Agent** 协作开发。编码前请阅读：

- [Agent须知/00-总览.md](./Agent须知/00-总览.md)
- [项目规划.md](./项目规划.md)
- 双语模块 UI：[Agent须知/03-双语模块UI规范.md](./Agent须知/03-双语模块UI规范.md)

前端 Agent 需对照 `Agent须知/UI参考/双语模块页面设计参考图.png` 验收双语页。

---

## MVP 进度

- [x] 前后端脚手架
- [x] 邮箱注册/登录 + JWT
- [x] 阅读/翻译章节
- [x] 翻译 AI 批改接口（占位 / 可接 LLM）
- [x] 双语视频列表 + 播放器 + 句轴
- [x] 社区 Feed（文字 + 图片）
- [x] VIP 字段与权限拦截
- [ ] 微信扫码登录
- [ ] VIP 支付
- [ ] 评论、词汇本
- [ ] 4K OSS 视频库
- [ ] Spring Cloud / 更多 AI 能力

---

## 常见问题

**Docker 启动失败**  
确认 Docker Desktop 已运行；或改用本地 MySQL/Redis 并修改 `application.yml`。

**前端无法请求后端**  
检查后端是否在 `8080` 运行，CORS 是否包含前端地址。

**Flyway 迁移失败**  
清空 `island` 库后重启后端，或检查 `backend/src/main/resources/db/migration/` 是否完整执行。

**B 站视频无法播放**  
大陆网络需能访问 B 站；句轴同步 MVP 为模拟进度，后期可接 B 站播放器 API。

---

## 相关文档

| 文档 | 说明 |
|------|------|
| [项目规划.md](./项目规划.md) | 产品边界、技术选型、MVP 范围 |
| [库表设计.md](./库表设计.md) | 表域索引（DDL 以 Flyway 为准） |
| [库表设计.sql](./库表设计.sql) | DDL 指引（非全量脚本） |
| [Agent须知/UI参考/视频源推荐.md](./Agent须知/UI参考/视频源推荐.md) | 推荐 Embed 视频与合规说明 |

---

## License

本项目为私有学习/产品项目，内容素材请遵守各自版权与平台 ToS。
