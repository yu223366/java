@echo off
chcp 936 >nul
setlocal
rem ============================================================
rem  ������� springBootTest����ɽ������֤���񣬶˿� 8080��
rem  ע�⣺������ʽָ�� --server.port=8080��
rem  ���򱾻� SERVER__PORT ���������Ḳ�����õ��°�ʧ�ܡ�
rem ============================================================
cd /d "%~dp0springBootTest"

rem ��������������
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo [����] ������� 8080 �˿����С�
    exit /b 0
)

rem ------------------------------------------------------------
rem  MySQL ���ṹ�ݵȲ��루2026-10-16 �޸�����
rem  ���߼�ֻ�� wushan ������ȱʧʱִ�� schema.sql�����¿��ڡ�
rem  ��ȱʧʱ�������ÿ���ֻ�� wushan_user����ҳ/�ؾ��ӿ�ȫ�� 500��
rem  ���߼����̽�⣬ȱ���ű�Ӧ���ĸ��ű���˳���������������
rem    wushan_user        �� schema.sql        ����֤��ɽ������
rem    wushan_category    �� schema-home.sql   ����ҳ������/Ʒ��/��Ŀ/����/�ȴ�/����/��ʱ/�۵�/������־��
rem    wushan_market_item �� upgrade-nav.sql   ���м���Ʒ/������־ + �۵�������������ӣ�
rem    wushan_spirit_root �� upgrade-codex.sql ���ؾ������/�ؾ���
rem    wushan_user.role   �� upgrade-role.sql  ��Ȩ���ֶΣ�0=����Ա 1=��ͨɽ�ѣ�ȱ��ʱ�Զ�����
rem ------------------------------------------------------------
set "MYSQL=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
set "DB=src\main\resources\db"
if exist "%MYSQL%" (
    "%MYSQL%" -uroot -p123456 -e "USE wushan" 2>nul
    if errorlevel 1 (
        echo [��ʼ��] δ��⵽ wushan ���ݿ⣬����ִ�� schema.sql ...
        "%MYSQL%" -uroot -p123456 --default-character-set=utf8mb4 < "%DB%\schema.sql"
    )

    call :ensure_table wushan_user        "%DB%\schema.sql"
    call :ensure_table wushan_category    "%DB%\schema-home.sql"
    call :ensure_table wushan_market_item "%DB%\upgrade-nav.sql"
    call :ensure_table wushan_spirit_root "%DB%\upgrade-codex.sql"
    call :ensure_column wushan_user role  "%DB%\upgrade-role.sql"
rem �ڶ����淨�����������ű�ȫ���ݵȣ�ÿ�������������� / �±� / ����
"%MYSQL%" -uroot -p123456 --default-character-set=utf8mb4 < "%DB%\upgrade-v2-items.sql" >nul 2>&1
"%MYSQL%" -uroot -p123456 --default-character-set=utf8mb4 < "%DB%\upgrade-v2-play.sql" >nul 2>&1
"%MYSQL%" -uroot -p123456 --default-character-set=utf8mb4 < "%DB%\upgrade-v2-admin.sql" >nul 2>&1
) else (
    echo [����] δ�ҵ� MySQL �ͻ��� %MYSQL%������������顣
    echo [����] ����ȱʧ����ҳ/�ؾ��ӿڽ��������������� MySQL80 ����
)

echo [����] ��˱��������У�target ��ɾ���Զ��ر��룬Լ 30 �룩...
rem ��վ���־��������һ�εı������������Ų�
if exist "_run.log" del /f /q "_run.log" >nul 2>&1
start "wushan-backend" /min cmd /c "mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8080 > _run.log 2>&1"
echo [���] ������ں�̨��������־��springBootTest\_run.log
exit /b 0

rem ------------------------------------------------------------
rem  �ӳ��򣺱���������ִ�ж�Ӧ SQL �ű�
rem    %1 = ����    %2 = SQL �ű�·������� springBootTest Ŀ¼��
rem ------------------------------------------------------------
:ensure_table
"%MYSQL%" -uroot -p123456 -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='wushan' AND table_name='%1'" 2>nul | findstr /b "1" >nul
if errorlevel 1 (
    echo [��ʼ��] ȱ�ٱ� %1������ִ�� %~nx3 ...
    "%MYSQL%" -uroot -p123456 --default-character-set=utf8mb4 < "%2"
)
exit /b 0

rem ------------------------------------------------------------
rem  �ӳ����в�������ִ�ж�Ӧ SQL �ű�
rem    %1 = ����    %2 = ����    %3 = SQL �ű�·������� springBootTest Ŀ¼��
rem ------------------------------------------------------------
:ensure_column
"%MYSQL%" -uroot -p123456 -N -e "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='wushan' AND table_name='%1' AND column_name='%2'" 2>nul | findstr /b "1" >nul
if errorlevel 1 (
    echo [��ʼ��] �� %1 ȱ���ֶ� %2������ִ�� %~nx3 ...
    "%MYSQL%" -uroot -p123456 --default-character-set=utf8mb4 < "%3"
)
exit /b 0
