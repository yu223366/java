# AI 速读 · 雾山五行全栈项目启动备忘

> 写给 WorkBuddy / AI 助手：每次开机后照此文档执行即可，不要重新推导。
> 本机每次重启会重置系统，`node_modules` 和 `target` 会被删除，其余（桌面项目、MySQL 数据、~/.m2 仓库、D:\nodejs）保留。

## 项目结构（相对本文件）

```
new-java-main/
├── springBootTest/              # 后端 Spring Boot（雾山五行认证服务）
│   ├── src/main/resources/db/schema.sql        # 建库 + wushan_user（认证）
│   ├── src/main/resources/db/schema-home.sql   # 首页 9 张表（分类/品阶/条目/导航/热词/公告/四时/论道/搜索日志）
│   ├── src/main/resources/db/upgrade-nav.sql   # 市集物品/交易日志 + 论道与分类扩充种子
│   ├── src/main/resources/db/upgrade-codex.sql # 藏经阁灵根/秘境表 + 种子
│   └── _run.log                 # 后端运行日志（脚本写入）
├── java-main/wushan-login-vue/  # 前端 Vue3 + Vite5（端口 5173，/api 代理到 8080）
│   └── _vite.log                # 前端运行日志（脚本写入）
├── 一键启动.cmd                  # 双击：MySQL检查→建库→后端→前端（GBK 编码，勿转 UTF-8）
├── 启动-后端.cmd / 启动-前端.cmd  # 单独启停
└── 停止项目.cmd                  # 按端口杀进程（8080/5173）
```

## 环境事实（已验证 2026-10-09）

- MySQL80 服务：自动启动，root/123456，库 `wushan`（表结构由 启动-后端.cmd 逐表探测幂等补齐，勿再手动执行 SQL）。
- Java 21（Zulu）在 PATH；无系统 Maven，用项目自带 `mvnw.cmd`（wrapper 与依赖缓存在 ~/.m2，重置后仍在）。
- Node：系统 `D:\Program Files\nodejs`（v20）；WorkBuddy 托管 v22 在 `~/.workbuddy/binaries/node/...`（重置后可能不在，脚本优先用 PATH 里的 npm）。
- 前端端口 5173，后端端口 8080。

## 关键坑（务必记住）

1. **`SERVER__PORT=54581` 环境变量劫持**：WorkBuddy shell 环境里有这个变量，Spring Boot 宽松绑定会把它当成 `server.port`，覆盖配置文件里的 8080。启动后端必须显式加：
   `-Dspring-boot.run.arguments=--server.port=8080`（命令行参数优先级高于环境变量）
2. **日志是 UTF-16**：`_run.log` 由 cmd 重定向生成，含 NUL 字符，读前先 `tr -d '\000'`。
3. **注册接口需要图形符印**：POST /api/auth/captcha 拿 `captchaId` + `devCode`（dev 环境明文回传），注册时一起提交。
4. **后端无 CORS 配置**：前端必须走 vite 代理（已配好），不要直接跨域调 8080。
5. **数据库「库在表缺」陷阱（2026-10-16 踩过）**：旧启动脚本只在 wushan 库整体缺失时执行 schema.sql；若库已存在但表缺失（重置快照只留 wushan_user），首页/藏经接口全部 500，前端各模块静默显示空态。现已改为逐表探测、按需应用 schema.sql → schema-home.sql → upgrade-nav.sql → upgrade-codex.sql（顺序有依赖）。**手动补库时也必须按此顺序，且加 `--default-character-set=utf8mb4` 防中文乱码。**
6. **前端跑真实后端模式**：`.env.local` 为 `VITE_USE_MOCK=false`，首页/藏经数据全部来自 8080；藏经阁接口契约为 `/api/codex/items|spirit-roots|realms`（后端 CodexController 已实现，2026-10-16 新增）。

## 手动启动命令（等价于脚本）

```bash
# 后端（在 springBootTest 目录）
./mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8080
# 前端（在 wushan-login-vue 目录，node_modules 被删则先 npm install）
npm install --no-audit --no-fund && npm run dev
```

## 验证清单

1. `netstat -ano | grep ":3306"` 有 LISTENING（MySQL）
2. `netstat -ano | grep ":8080"` 有 LISTENING（后端，日志出现 `Started SpringBootTestApplication`）
3. `netstat -ano | grep ":5173"` 有 LISTENING（前端）
4. 端到端：`curl -X POST http://localhost:5173/api/auth/captcha` 返回 `{"code":0,...}`
5. 首页三接口：`curl -s localhost:8080/api/home/nav`、`/api/home/discourse/list?page=1&size=8`、`/api/home/market/items?page=1&size=12` 均返回 `code:0`
6. 藏经三接口：`curl -s localhost:8080/api/codex/items`、`/api/codex/spirit-roots`、`/api/codex/realms` 均返回 `code:0`

## 已知小毛病（不影响运行，未修）

- `application.properties` 里 `logging.pattern.file` 的值误写成了 `logging.pattern.console=...`（key 重复嵌套），日志格式带前缀但能用。
- `application-prob.properties` 文件名应为 `prod`（拼写错误）。
- 后端无单元测试、前端无 .gitignore。
