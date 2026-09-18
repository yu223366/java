# 雾山五行 · 入境（Vue 版）

将原桌面 `login-wushan.html` 单文件登录页，重构为 **Vue 3 + Vite** 工程。
视觉与交互完全保留：雾气山峦 SVG、Canvas 火星粒子背景、五行换主题色、名号暗语校验、登录「落印」动画。

## 目录结构

```
wushan-login-vue/
├── index.html
├── package.json
├── vite.config.js
└── src/
    ├── main.js
    ├── style.css
    ├── App.vue                 # 根布局 + 五行主题色状态提升
    └── components/
        ├── EmberBackground.vue # 雾 + 火墨 粒子背景（Canvas）
        ├── FogMountains.vue    # 雾气山峦 SVG
        └── LoginPanel.vue      # 登录面板：五行 / 表单 / 成功页
```

## 运行

```bash
npm install      # 安装依赖
npm run dev      # 本地开发，默认 http://localhost:5173
npm run build    # 打包到 dist/
npm run preview  # 预览打包结果
```

## 与原文件的对应

| 原 login-wushan.html | Vue 组件 |
| --- | --- |
| `<style>` 全局变量/body | `src/style.css` |
| `<canvas>` 粒子脚本 | `EmberBackground.vue` |
| 雾气山峦 `<svg>` | `FogMountains.vue` |
| `.panel` 登录卡片 | `LoginPanel.vue` |
| 五行换色 / 登录逻辑 | `App.vue` 状态 + `LoginPanel.vue` 事件 |
