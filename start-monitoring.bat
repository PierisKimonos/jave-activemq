@echo off
echo 🚀 Starting Demo MQ Service with Monitoring...

echo 📊 Starting monitoring stack (Prometheus + Grafana)...
docker-compose --profile monitoring up -d

echo ⏳ Waiting for monitoring services to be ready...
timeout /t 15 /nobreak >nul

echo 🔧 Starting ActiveMQ...
docker-compose up -d activemq

echo ⏳ Waiting for ActiveMQ to be ready...
timeout /t 20 /nobreak >nul

REM Check if ActiveMQ is running by checking the container status
docker-compose ps activemq | findstr "demo-activemq" | findstr "Up" >nul
if %errorlevel% == 0 (
    echo ✅ All services are running!
    echo.
    echo 📊 Monitoring URLs:
    echo    - Grafana Dashboard: http://localhost:3000 (admin/admin)
    echo    - Prometheus: http://localhost:9090
    echo    - ActiveMQ Console: http://localhost:8161/console/ (admin/admin)
    echo.
    echo 🔌 JMS Connection URL: tcp://localhost:61616
    echo.
    echo 🧪 Next steps:
    echo    1. Start your Spring Boot app: mvn spring-boot:run
    echo    2. Test the service: .\test-service.bat
    echo    3. Monitor in Grafana: http://localhost:3000
    echo.
    echo 📋 To see all running containers: docker-compose ps
) else (
    echo ❌ ActiveMQ failed to start properly
    echo 📋 Container status:
    docker-compose ps
    echo.
    echo 📋 ActiveMQ logs:
    docker-compose logs activemq
)
