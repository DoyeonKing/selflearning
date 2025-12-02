@echo off
echo ========================================
echo 网络连接测试工具
echo ========================================
echo.

echo 1. 检查后端服务是否运行...
netstat -ano | findstr :8080
if %errorlevel% == 0 (
    echo [OK] 后端服务正在运行
) else (
    echo [错误] 后端服务未运行，请先启动SpringBoot
    pause
    exit
)

echo.
echo 2. 你的电脑IP地址：
ipconfig | findstr /i "IPv4"

echo.
echo 3. 测试连接（需要手机和电脑在同一WiFi）：
echo    请在手机浏览器访问：http://172.20.10.3:8080
echo    或者：http://192.168.137.1:8080
echo.

echo 4. 如果连接失败，检查防火墙：
echo    控制面板 - Windows Defender 防火墙 - 允许应用通过防火墙
echo    确保"Java"或"8080端口"被允许
echo.

pause











