@echo off
echo 🚀 Starting ActiveMQ with Docker...

REM Start only ActiveMQ
docker-compose up -d activemq

echo ⏳ Waiting for ActiveMQ to be ready...
timeout /t 15 /nobreak >nul

REM Check if ActiveMQ is running
docker-compose ps activemq | findstr "demo-activemq" | findstr "Up" >nul
if %errorlevel% == 0 (
    echo ✅ ActiveMQ is running!
    echo.
    echo 📊 ActiveMQ Web Console: http://localhost:8161/console/
    echo 👤 Username: admin
    echo 🔑 Password: admin
    echo.
    echo 🔌 JMS Connection URL: tcp://localhost:61616
    echo.
    echo 🧪 Test your Spring Boot app by running: mvn spring-boot:run
    echo 📨 Then call: curl http://localhost:8080/api/messages/publish
    echo.
    echo 📋 To see container status: docker-compose ps activemq
) else (
    echo ❌ Failed to start ActiveMQ
    echo 📋 Container status:
    docker-compose ps activemq
    echo.
    echo 📋 ActiveMQ logs:
    docker-compose logs activemq
)
