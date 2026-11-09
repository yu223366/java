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
    echo [就绪] 前端  http://192.168.68.38:5173
    start "" "http://192.168.68.38:5173"
)
rem ------------------------------------------------------------
rem  前端「假就绪」排查（2026-10-26 新增）：
rem  Vite 即使模块解析失败（Failed to resolve import）也会照常监听
rem  5173，脚本只查端口会误报「就绪」，页面却一片空白。
rem  这里再扫一遍 _vite.log，命中典型编译错误就显式告警。
rem ------------------------------------------------------------
set "VITE_LOG=%~dp0java-main\wushan-login-vue\_vite.log"
if exist "%VITE_LOG%" (
    findstr /i /c:"Failed to resolve import" /c:"Internal server error" /c:"Pre-transform error" "%VITE_LOG%" >nul
    if not errorlevel 1 (
        echo [异常] 前端已监听端口，但页面编译报错（多为缺文件或路径写错）。
        echo [异常] 详见 wushan-login-vue\_vite.log
    )
)
echo.
pause
