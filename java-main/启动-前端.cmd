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

rem 定位 npm：优先 PATH，其次自动探测本机常见的 Node 安装位置。
rem 本机系统 PATH 中没有 Node，AI-README 里记的 D:\Program Files\nodejs 也已不存在，
rem 故按「托管 Node -> DevEco 自带 -> 常规安装目录」依次探测，换机器/换版本都不用改脚本。
set "NODE_DIR="
where npm >nul 2>nul
if not errorlevel 1 goto npm_ready

for /f "delims=" %%F in ('dir /b /s "%USERPROFILE%\.workbuddy\binaries\node\npm.cmd" 2^>nul') do if not defined NODE_DIR set "NODE_DIR=%%~dpF"
if not defined NODE_DIR if exist "D:\DevEco Studio\tools\node\npm.cmd" set "NODE_DIR=D:\DevEco Studio\tools\node\"
if not defined NODE_DIR if exist "D:\Program Files\nodejs\npm.cmd" set "NODE_DIR=D:\Program Files\nodejs\"
if not defined NODE_DIR if exist "C:\Program Files\nodejs\npm.cmd" set "NODE_DIR=C:\Program Files\nodejs\"
if not defined NODE_DIR if exist "%APPDATA%\nvm\npm.cmd" set "NODE_DIR=%APPDATA%\nvm\"

if not defined NODE_DIR (
    echo [错误] 找不到 npm，请确认 Node.js 已安装。
    pause
    exit /b 1
)
set "PATH=%NODE_DIR%;%PATH%"
:npm_ready
if defined NODE_DIR echo [信息] 已自动定位 Node 目录：%NODE_DIR%

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
rem 清空旧日志：否则上一次的报错会混进本次排查，误报「编译失败」
if exist "_vite.log" del /f /q "_vite.log" >nul 2>&1
start "wushan-frontend" /min cmd /c "npm run dev > _vite.log 2>&1"
echo [完成] 前端已在后台启动，日志：wushan-login-vue\_vite.log
