@echo off
chcp 65001 > nul
echo ========================================
echo     数据库字段检查与更新工具
echo ========================================
echo.

:: 检查MySQL是否在PATH中
where mysql >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [错误] 找不到mysql命令，请确保MySQL已安装并添加到PATH
    echo 或者手动在MySQL客户端执行以下文件：
    echo   1. check_db_fields.sql ^(检查字段^)
    echo   2. sql语句\修改语句\add_qrcode_to_map_nodes.sql ^(添加字段^)
    pause
    exit /b 1
)

echo [步骤 1/3] 检查数据库字段...
echo ----------------------------------------
echo.

set /p username="请输入MySQL用户名 (默认: root): "
if "%username%"=="" set username=root

echo.
echo 正在检查 map_nodes 表的字段...
mysql -u %username% -p hospital_system < check_db_fields.sql

echo.
echo ----------------------------------------
echo [步骤 2/3] 查看检查结果
echo ----------------------------------------
echo.
echo 如果上面显示了4个二维码相关字段，说明字段已存在，无需更新。
echo 如果字段数量少于4个，请继续执行更新。
echo.
set /p continue="是否继续执行字段更新？(Y/N): "
if /i not "%continue%"=="Y" (
    echo 已取消更新。
    pause
    exit /b 0
)

echo.
echo ----------------------------------------
echo [步骤 3/3] 执行数据库字段更新
echo ----------------------------------------
echo.
echo 正在执行更新脚本...
mysql -u %username% -p hospital_system < "sql语句\修改语句\add_qrcode_to_map_nodes.sql"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo  ✓ 数据库字段更新成功！
    echo ========================================
    echo.
    echo 接下来需要：
    echo 1. 重启 Spring Boot 应用
    echo 2. 测试导航功能是否正常
    echo 3. 查看二维码生成功能是否可用
    echo.
) else (
    echo.
    echo ========================================
    echo  × 更新失败，请检查错误信息
    echo ========================================
    echo.
    echo 可能的原因：
    echo 1. 数据库连接失败
    echo 2. 字段已存在（重复执行）
    echo 3. 权限不足
    echo.
)

pause





