@echo off
REM Quick start script for the Demo ActiveMQ Spring Boot project

echo ========================================
echo Demo ActiveMQ Spring Boot Project
echo ========================================
echo.
echo Available options:
echo 1. Start ActiveMQ only (simple)
echo 2. Start ActiveMQ + Monitoring (Prometheus, Grafana)
echo 3. Start everything including Spring Boot app
echo 4. Stop all services
echo 5. View logs
echo 6. Clean up (remove volumes)
echo.

set /p choice="Enter your choice (1-6): "

if "%choice%"=="1" (
    echo Starting ActiveMQ only...
    docker-compose -f docker-compose.yml up -d
    echo.
    echo ActiveMQ started!
    echo Web Console: http://localhost:8161/console
    echo JMS Port: 61616
    echo JMX Port: 1099
    echo.
    echo Login credentials: admin/admin
) else if "%choice%"=="2" (
    echo Starting ActiveMQ with monitoring...
    docker-compose up -d activemq --profile monitoring
    echo.
    echo Services started!
    echo ActiveMQ Console: http://localhost:8161/console
    echo Prometheus: http://localhost:9090
    echo Grafana: http://localhost:3000 (admin/admin)
) else if "%choice%"=="3" (
    echo Starting all services...
    docker-compose up -d --profile app --profile monitoring
    echo.
    echo All services started!
    echo Spring Boot App: http://localhost:8080/dashboard
    echo ActiveMQ Console: http://localhost:8161/console
    echo Prometheus: http://localhost:9090
    echo Grafana: http://localhost:3000 (admin/admin)
) else if "%choice%"=="4" (
    echo Stopping all services...
    docker-compose down
    docker-compose -f docker-compose.yml down
    echo All services stopped.
) else if "%choice%"=="5" (
    echo Select service to view logs:
    echo 1. ActiveMQ
    echo 2. Spring Boot App
    echo 3. Prometheus
    echo 4. Grafana
    set /p log_choice="Enter choice (1-4): "
    
    if "%log_choice%"=="1" docker logs -f demo-activemq
    if "%log_choice%"=="2" docker logs -f demo-spring-app
    if "%log_choice%"=="3" docker logs -f demo-prometheus
    if "%log_choice%"=="4" docker logs -f demo-grafana
) else if "%choice%"=="6" (
    echo WARNING: This will remove all data!
    set /p confirm="Are you sure? (y/N): "
    if /i "%confirm%"=="y" (
        docker-compose down -v
        docker-compose -f docker-compose.yml down -v
        echo All volumes removed.
    )
) else (
    echo Invalid choice. Please run the script again.
)

echo.
pause
