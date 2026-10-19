@echo off
chcp 936 >nul
setlocal
rem ============================================================
rem  启动后端 springBootTest（雾山五行认证服务，端口 8080）
rem  注意：必须显式指定 --server.port=8080，
rem  否则本机 SERVER__PORT 环境变量会覆盖配置导致绑定失败。
rem ============================================================
cd /d "%~dp0springBootTest"

rem 已在运行则跳过
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo [跳过] 后端已在 8080 端口运行。
    exit /b 0
)

rem ------------------------------------------------------------
rem  MySQL 表结构幂等补齐（2026-10-16 修复）：
rem  旧逻辑只在 wushan 库整体缺失时执行 schema.sql，导致库在、
rem  表缺失时（如重置快照只留 wushan_user）首页/藏经接口全部 500。
rem  新逻辑逐表探测，缺哪张表应用哪个脚本，顺序有依赖勿调换：
rem    wushan_user        → schema.sql        （认证：山籍表）
rem    wushan_category    → schema-home.sql   （首页：分类/品阶/条目/导航/热词/公告/四时/论道/搜索日志）
rem    wushan_market_item → upgrade-nav.sql   （市集物品/交易日志 + 论道与分类扩充种子）
rem    wushan_spirit_root → upgrade-codex.sql （藏经阁：灵根/秘境）
rem ------------------------------------------------------------
set "MYSQL=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
set "DB=src\main\resources\db"
if exist "%MYSQL%" (
    "%MYSQL%" -uroot -p123456 -e "USE wushan" 2>nul
    if errorlevel 1 (
        echo [初始化] 未检测到 wushan 数据库，正在执行 schema.sql ...
        "%MYSQL%" -uroot -p123456 --default-character-set=utf8mb4 < "%DB%\schema.sql"
    )

    call :ensure_table wushan_user        "%DB%\schema.sql"
    call :ensure_table wushan_category    "%DB%\schema-home.sql"
    call :ensure_table wushan_market_item "%DB%\upgrade-nav.sql"
    call :ensure_table wushan_spirit_root "%DB%\upgrade-codex.sql"
) else (
    echo [警告] 未找到 MySQL 客户端 %MYSQL%，跳过建表检查。
    echo [警告] 若表缺失，首页/藏经接口将报错，请先启动 MySQL80 服务。
)

echo [启动] 后端编译启动中（target 被删会自动重编译，约 30 秒）...
start "wushan-backend" /min cmd /c "mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8080 > _run.log 2>&1"
echo [完成] 后端已在后台启动，日志：springBootTest\_run.log
exit /b 0

rem ------------------------------------------------------------
rem  子程序：表不存在则执行对应 SQL 脚本
rem    %1 = 表名    %2 = SQL 脚本路径（相对 springBootTest 目录）
rem ------------------------------------------------------------
:ensure_table
"%MYSQL%" -uroot -p123456 -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='wushan' AND table_name='%1'" 2>nul | findstr /b "1" >nul
if errorlevel 1 (
    echo [初始化] 缺少表 %1，正在执行 %~nx2 ...
    "%MYSQL%" -uroot -p123456 --default-character-set=utf8mb4 < "%2"
)
exit /b 0
