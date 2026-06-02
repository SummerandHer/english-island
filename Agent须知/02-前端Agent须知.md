# 前端 Agent 须知

> **每次编码前**必须阅读本目录全部文件，并对照 `双语模块页面设计参考图.png` 与 `image.png`。

## 1. 技术栈（已确定）

| 项 | 选型 |
|----|------|
| 框架 | **Nuxt 3** + Vue 3 + TypeScript |
| 样式 | Tailwind CSS |
| 组件库 | Naive UI（清新校园风，主色建议青绿 `#18a058` 系） |
| 状态 | Pinia |
| HTTP | `$fetch` / ofetch 封装 |
| 图标 | @iconify/vue 或 Naive 内置 |

### 为何不用 React/Next.js

- 更小基线包体，首屏更轻
- 3 天 MVP 约定式目录更快落地
- 技巧章节适合 SSG/SSR 混合

## 2. 目录结构

```
frontend/
├── app.vue
├── nuxt.config.ts
├── assets/css/main.css
├── components/
│   ├── layout/AppHeader.vue      # B 站式顶栏
│   ├── community/PostCard.vue
│   └── video/
│       ├── VideoPlayer.vue
│       ├── VideoControls.vue
│       ├── CurrentSentence.vue
│       └── SentenceList.vue
├── composables/
│   ├── useAuth.ts
│   ├── useVideoSync.ts           # 句级同步核心
│   └── useApi.ts
├── pages/
│   ├── index.vue                 # 首页
│   ├── feed/index.vue            # 全平台
│   ├── reading/[slug].vue
│   ├── translation/[slug].vue
│   ├── video/index.vue
│   ├── video/[id].vue            # 双语学习页
│   └── login.vue
└── layouts/default.vue
```

## 3. 视觉规范 · 清新校园

- 背景：浅灰白 `#f7f8fa`，卡片白底 + 轻阴影
- 圆角：卡片 `12px`，按钮 `8px`
- 字体：系统栈 `-apple-system, "PingFang SC", "Microsoft YaHei", sans-serif`
- 正文：`15–16px`，行高 `1.6`
- 顶栏：固定顶部，白底，模块 Tab + 用户头像
- 登录页：左右分栏，左侧品牌卖点 + 右侧微信扫码（见 `image.png`）

## 4. 路由与顶栏

| 导航 | 路径 | 说明 |
|------|------|------|
| 首页 | `/` | 模块入口 |
| 全平台 | `/feed` | 社区 Feed |
| 阅读 | `/reading` | 章节列表 |
| 翻译 | `/translation` | 章节 + 练习 |
| 双语 | `/video` | 视频列表 |

顶栏参考 B 站：Logo + 导航项 + 搜索（MVP 可占位）+ 登录/头像。

## 5. 性能要求（轻量化）

- 路由级 code splitting，视频页不加载社区大组件
- 图片：`NuxtImg` 或 lazy load，Feed 缩略图宽度 ≤ 800
- 技巧章节：构建时 SSG 或 ISR，减少 TTFB
- 第三方 Embed 脚本：**动态 import**，仅视频页加载
- 目标：LCP < 2.5s（4G 模拟），首页 JS gzip < 200KB（不含 Embed）

## 6. 响应式

- 断点：`md:768px` `lg:1024px`
- 双语页：桌面左右分栏；平板/窄屏改为**上视频下字幕列表**堆叠
- 控制栏图标换行时保持可点区域 ≥ 44px

## 7. 鉴权与 VIP

- Token 存 `httpOnly` cookie（优先）或 localStorage（MVP 可简化）
- VIP 内容：接口 403 时展示升级提示，不硬编码绕过
- 翻译 AI 批改：提交后 loading + 分块展示评分与建议

## 8. 社区 Feed

- 发帖：textarea + 多图上传预览
- **禁止** UI 上出现视频上传、外链输入（产品约束）
- 列表：卡片流，图集网格展示

## 9. 页面清单

### MVP（已交付）

- [x] 首页 + 顶栏、阅读 2 章、翻译 2 章 + 列表页内 1 题 AI 批改
- [x] 视频列表 + 双语学习页（见 03 规范）、Feed、登录/注册、Admin 视频

### Phase 2（见 [04-阅读翻译Phase2规划.md](./04-阅读翻译Phase2规划.md)）

- [ ] `/reading/practice/[passageId]` 模拟阅读（选择题 + 解析）
- [x] `/translation/practice/[id]` 独立翻译练习页
- [x] 批改结果：15 分制 + 分档标签 + 提交历史（`/translation` 列表页）
- [ ] 统一 `VipUpgradeBanner`（替换 `alert`）

## 10. 禁止事项

- ❌ 擅自换成 React/其他框架
- ❌ 引入重型 UI 库（Ant Design 全量等）
- ❌ 在前端下载/缓存第三方视频文件
- ❌ 偏离双语参考图的布局结构（除非与产品负责人确认）
