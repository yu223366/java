# 雾山五行 · 入境 / 立契（Vue 版）

将原桌面 `login-wushan.html` 单文件登录页，重构为 **Vue 3 + Vite** 工程，并新增**五行风格注册页**。
视觉与交互延续：雾气山峦 SVG、Canvas 火星粒子背景、五行换主题色、水墨暗调面板、落印动画。

- 登录：`雾山 · 入境` —— 名号 + 暗语，五行换色
- 注册：`立契 · 入籍` —— 单页制，名号查重 / 暗语强度 / 图形符印同屏校验，后端接口已预留
  （已取消邮箱 / 手机号与六爻密印，无需任何短信邮件通道）

## 目录结构

```
wushan-login-vue/
├── index.html
├── package.json
├── vite.config.js
├── .env.example              # 后端地址 / mock 开关（复制为 .env.local 使用）
└── src/
    ├── main.js
    ├── style.css
    ├── App.vue                 # 根布局 + 五行主题色状态提升 + 登录/注册视图切换
    ├── api/
    │   └── auth.js             # ★ 后端接口预留层（captcha / 查重 / 注册 / 登录）
    ├── constants/
    │   └── wuxing.js           # 五行常量（色值 / 本命描述）
    ├── utils/
    │   ├── validators.js       # ★ 表单校验规则（名号 / 暗语 / 图形符印）
    │   └── captcha.js          # 图形验证码本地绘制（mock 模式）
    └── components/
        ├── EmberBackground.vue # 雾 + 火墨 粒子背景（Canvas）
        ├── FogMountains.vue    # 雾气山峦 SVG
        ├── LoginPanel.vue      # 登录面板
        ├── RegisterPanel.vue   # ★ 注册面板（单页制 · 立契）
        ├── WuxingPicker.vue    # ★ 五行选择器（登录/注册共用）
        ├── CaptchaBox.vue      # ★ 图形验证码（本地 Canvas / 后端图片双模式）
        └── OtpInput.vue        # 六爻密印输入（六格分离式验证码，当前注册流程未启用，保留备用）
```

## 运行

```bash
npm install      # 安装依赖
npm run dev      # 本地开发，默认 http://localhost:5173
npm run build    # 打包到 dist/
npm run preview  # 预览打包结果
```

## 注册页（立契）说明

### 交互流程（单页制 · 一屏填完）

| 顺序 | 内容 |
| --- | --- |
| 1 | 本命五行（点选后全局主题色同步变化） |
| 2 | 名号（失焦即后端查重，显示「山籍可用 / 已被占用」） |
| 3 | 暗语（强度条 + 四规则实时打勾） |
| 4 | 重录暗语（一致则显示「一致」） |
| 5 | 图形符印（点击图片换一张） |
| 6 | 允诺山规 → 点「立契」提交 |

回车（名号 / 重录暗语输入框）等同于点「立契」；提交时名号、暗语、符印、盟约一次性全量校验。

### 密码（暗语）校验

| 规则 | 说明 |
| --- | --- |
| 长度 | 8 ~ 20 位 |
| 组成 | 必须同时包含字母与数字 |
| 禁止 | 空格、汉字；与名号相同或包含名号 |
| 强度 | 五级：弱水 / 薄弱 / 尚可 / 稳固 / 金汤（含符号再 +1 级） |
| 一致性 | 重录暗语需与暗语完全一致 |

四条约规则在页面上实时打勾（`src/utils/validators.js` 的 `PASSWORD_RULES`）。

### 验证码校验

1. **图形符印**：mock 模式下由 Canvas 本地绘制（排除易混字符 0/O/1/I/2/Z/8/B），点击图片换一张；填错立即提示并自动换新符。
2. **后端比对**：接后端后图形符印由服务端生成 base64 图片（`/auth/captcha`），前端只做非空 / 位数校验，
   比对交由后端；注册提交前会先调 `/auth/captcha/verify` 预校验，再由 `/auth/register` 做最终比对并作废该符印。
3. 注册失败（名号重复、符印不合等）返回后，前端会自动换一张新符印，并展示后端返回的中文错误文案。

## 后端接口预留（`src/api/auth.js`）

统一响应体约定：`{ code: 0, message: 'ok', data: {...} }`，`code !== 0` 视为失败。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/auth/captcha` | 生成图形验证码，返回 `{ captchaId, image }` |
| POST | `/auth/captcha/verify` | 校验图形验证码 `{ captchaId, captchaCode }` |
| GET | `/auth/username/available?username=` | 名号可用性 `{ available }` |
| POST | `/auth/register` | 注册（见下方请求体） |
| POST | `/auth/login` | 登录（登录页预留） |

注册请求体：

```json
{
  "username": "雾山甲",
  "password": "Wushan2026",
  "element": "huo",
  "elementLabel": "火",
  "captchaId": "cap_xxx",
  "captchaCode": "A7CQ",
  "agreed": true,
  "agreedAt": "2026-09-21T10:55:18.000Z"
}
```

### 接入真实后端

1. 复制 `.env.example` 为 `.env.local`，设置：

   ```
   VITE_API_BASE_URL=https://your-api.com/api
   VITE_USE_MOCK=false
   ```

2. 若要走后端图形验证码，把 `authApi.getCaptcha()` 拿到的图片地址传给 `RegisterPanel` 中方框：`<CaptchaBox :image-src="capImage" captcha-id="capId" />`，此时前端只做非空/长度校验，比对全部交给后端。
3. token 存取已内置：`tokenStore`（`src/api/auth.js`），登录/注册拿到 token 后调用 `tokenStore.set(res.token)` 即可。

## 与原文件的对应

| 原 login-wushan.html | Vue 组件 |
| --- | --- |
| `<style>` 全局变量 / body | `src/style.css` |
| `<canvas>` 粒子脚本 | `EmberBackground.vue` |
| 雾气山峦 `<svg>` | `FogMountains.vue` |
| `.panel` 登录卡片 | `LoginPanel.vue` |
| 五行换色 / 登录逻辑 | `App.vue` 状态 + `LoginPanel.vue` 事件 |
| 注册页（新增） | `RegisterPanel.vue` + `WuxingPicker` / `CaptchaBox` |
