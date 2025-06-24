@echo off
echo 🧪 Testing ActiveMQ Artemis Console Login...
echo.
echo 🌐 Opening ActiveMQ Console: http://localhost:8161/console/
echo 👤 Username: admin
echo 🔑 Password: admin
echo.
echo 🔍 Checking if ActiveMQ is running...

docker-compose -f docker-compose.yml ps activemq | findstr "demo-activemq" | findstr "Up" >nul
if %errorlevel% == 0 (
    echo ✅ ActiveMQ is running!
    echo.
    echo 📝 Login Credentials:
    echo    Username: admin
    echo    Password: admin
    echo.
    echo 🚀 Try logging in at: http://localhost:8161/console/
    echo.
    echo 📊 If login still fails, let's try the emergency fix:
    echo 🔧 We'll recreate ActiveMQ with default authentication
    echo.
    set /p choice="Want to try the emergency fix? (y/n): "
    if /i "%choice%"=="y" (
        echo.
        echo 🛠️  Applying emergency fix...
        docker-compose -f docker-compose.yml down activemq
        docker volume rm demo_activemq_data 2>nul
        echo � Removed old data volume
        docker-compose -f docker-compose.yml up -d activemq
        echo ⏳ Waiting for ActiveMQ to start...
        timeout /t 15 /nobreak >nul
        echo.
        echo 🎯 Try now: http://localhost:8161/console/
        echo 👤 Username: admin  
        echo 🔑 Password: admin
    )
) else (
    echo ❌ ActiveMQ is not running!
    echo 🔧 Start it with: docker-compose -f docker-compose.yml up -d activemq
)

echo.
pause
