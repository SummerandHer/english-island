# ISLAND 前端

Nuxt 3 + Vue 3 + TypeScript 前端工程。完整项目说明见根目录 [README.md](../README.md)。

## 常用命令

```bash
npm install      # 安装依赖
npm run dev      # 开发 http://localhost:3000
npm run build    # 生产构建
npm run preview  # 预览构建结果
```

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `NUXT_PUBLIC_API_BASE` | `http://localhost:8080` | 后端 API 地址 |

## 目录说明

```
frontend/
├── pages/           # 路由页面
├── components/      # 组件（layout / video）
├── composables/     # useApi、useVideoSync
├── stores/          # Pinia（auth）
├── layouts/         # 默认布局 + 顶栏
├── plugins/         # Naive UI
└── assets/css/      # 全局样式
```

## UI 规范

开发前请阅读 [Agent须知](../Agent须知/) 目录，双语模块对照 `UI参考/双语模块页面设计参考图.png`。
