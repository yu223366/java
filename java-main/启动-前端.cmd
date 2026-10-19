@echo off
chcp 936 >nul
setlocal
rem ============================================================
rem  启动前端 wushan-login-vue（Vite，端口 5173）
rem  node_modules 每次开机会被删除，脚本会先检测并重装依赖。
rem ============================================================
cd /d "%~dp0java-main\wushan-login-vue"

netstat -ano | findstr ":5173" | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo [跳过] 前端已在 5173 端口运行。
    exit /b 0
)

rem 定位 npm：优先 PATH，其次系统安装目录
where npm >nul 2>nul
if errorlevel 1 (
    if exist "D:\Program Files\nodejs\npm.cmd" (
        set "PATH=D:\Program Files\nodejs;%PATH%"
    ) else (
        echo [错误] 找不到 npm，请确认 Node.js 已安装。
        pause
        exit /b 1
    )
)

if not exist node_modules (
    echo [安装] node_modules 不存在，正在安装依赖（首次约 1-3 分钟）...
    call npm install --no-audit --no-fund
    if errorlevel 1 (
        echo [错误] 依赖安装失败。
        pause
        exit /b 1
    )
)

echo [启动] 前端开发服务器启动中...
start "wushan-frontend" /min cmd /c "npm run dev > _vite.log 2>&1"
echo [完成] 前端已在后台启动，日志：wushan-login-vue\_vite.log
