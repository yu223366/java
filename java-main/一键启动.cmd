@echo off
chcp 936 >nul
setlocal
rem ============================================================
rem  雾山五行项目 · 一键启动（MySQL 探测 + 后端 + 前端）
rem  开机后双击本文件即可。详见 AI-README.md
rem ============================================================
echo ========== 1/3 探测 MySQL (3306) ==========
netstat -ano | findstr ":3306" | findstr "LISTENING" >nul
if errorlevel 1 (
    echo [警告] 未检测到 3306 端口监听，请确认 MySQL80 服务已启动！
) else (
    echo [正常] MySQL 正在监听 3306。
)

echo ========== 2/3 启动后端 ==========
call "%~dp0启动-后端.cmd"

echo ========== 3/3 启动前端 ==========
call "%~dp0启动-前端.cmd"

echo ========== 等待服务就绪（最多 2 分钟） ==========
set /a N=0
:wait_loop
set /a N+=1
if %N% gtr 24 goto wait_done
ping -n 6 127.0.0.1 >nul
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if errorlevel 1 goto wait_loop
netstat -ano | findstr ":5173" | findstr "LISTENING" >nul
if errorlevel 1 goto wait_loop
:wait_done

netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if errorlevel 1 (
    echo [警告] 后端 8080 未就绪，请查看 springBootTest\_run.log
) else (
    echo [就绪] 后端  http://localhost:8080
)
netstat -ano | findstr ":5173" | findstr "LISTENING" >nul
if errorlevel 1 (
    echo [警告] 前端 5173 未就绪，请查看 wushan-login-vue\_vite.log
) else (
    echo [就绪] 前端  http://localhost:5173
    start "" "http://localhost:5173"
)
echo.
pause
