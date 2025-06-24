#!/usr/bin/env pwsh

# Quick start script for the Demo ActiveMQ Spring Boot project

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Demo ActiveMQ Spring Boot Project" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Available options:" -ForegroundColor Yellow
Write-Host "1. Start ActiveMQ only (simple)" -ForegroundColor White
Write-Host "2. Start ActiveMQ + Monitoring (Prometheus, Grafana)" -ForegroundColor White
Write-Host "3. Start everything including Spring Boot app" -ForegroundColor White
Write-Host "4. Stop all services" -ForegroundColor White
Write-Host "5. View logs" -ForegroundColor White
Write-Host "6. Clean up (remove volumes)" -ForegroundColor White
Write-Host "7. Test connections" -ForegroundColor White
Write-Host ""

$choice = Read-Host "Enter your choice (1-7)"

switch ($choice) {
    "1" {
        Write-Host "Starting ActiveMQ only..." -ForegroundColor Green
        docker-compose -f docker-compose.yml up -d
        Write-Host ""
        Write-Host "ActiveMQ started!" -ForegroundColor Green
        Write-Host "Web Console: http://localhost:8161/console" -ForegroundColor Cyan
        Write-Host "JMS Port: 61616" -ForegroundColor Cyan
        Write-Host "JMX Port: 1099" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Login credentials: admin/admin" -ForegroundColor Yellow
    }
    "2" {
        Write-Host "Starting ActiveMQ with monitoring..." -ForegroundColor Green
        docker-compose up -d activemq
        docker-compose --profile monitoring up -d
        Write-Host ""
        Write-Host "Services started!" -ForegroundColor Green
        Write-Host "ActiveMQ Console: http://localhost:8161/console" -ForegroundColor Cyan
        Write-Host "Prometheus: http://localhost:9090" -ForegroundColor Cyan
        Write-Host "Grafana: http://localhost:3000 (admin/admin)" -ForegroundColor Cyan
    }
    "3" {
        Write-Host "Starting all services..." -ForegroundColor Green
        docker-compose --profile app --profile monitoring up -d
        Write-Host ""
        Write-Host "All services started!" -ForegroundColor Green
        Write-Host "Spring Boot App: http://localhost:8080/dashboard" -ForegroundColor Cyan
        Write-Host "ActiveMQ Console: http://localhost:8161/console" -ForegroundColor Cyan
        Write-Host "Prometheus: http://localhost:9090" -ForegroundColor Cyan
        Write-Host "Grafana: http://localhost:3000 (admin/admin)" -ForegroundColor Cyan
    }
    "4" {
        Write-Host "Stopping all services..." -ForegroundColor Yellow
        docker-compose down
        docker-compose -f docker-compose.yml down
        Write-Host "All services stopped." -ForegroundColor Green
    }
    "5" {
        Write-Host "Select service to view logs:" -ForegroundColor Yellow
        Write-Host "1. ActiveMQ"
        Write-Host "2. Spring Boot App"
        Write-Host "3. Prometheus"
        Write-Host "4. Grafana"
        $logChoice = Read-Host "Enter choice (1-4)"
        
        switch ($logChoice) {
            "1" { docker logs -f demo-activemq }
            "2" { docker logs -f demo-spring-app }
            "3" { docker logs -f demo-prometheus }
            "4" { docker logs -f demo-grafana }
            default { Write-Host "Invalid choice" -ForegroundColor Red }
        }
    }
    "6" {
        Write-Host "WARNING: This will remove all data!" -ForegroundColor Red
        $confirm = Read-Host "Are you sure? (y/N)"
        if ($confirm -eq "y" -or $confirm -eq "Y") {
            docker-compose down -v
            docker-compose -f docker-compose.yml down -v
            Write-Host "All volumes removed." -ForegroundColor Green
        }
    }
    "7" {
        Write-Host "Testing connections..." -ForegroundColor Yellow
        Write-Host "Testing ActiveMQ JMS port (61616)..."
        Test-NetConnection -ComputerName localhost -Port 61616
        Write-Host "Testing ActiveMQ Web Console (8161)..."
        Test-NetConnection -ComputerName localhost -Port 8161
        Write-Host "Testing ActiveMQ JMX port (1099)..."
        Test-NetConnection -ComputerName localhost -Port 1099
    }
    default {
        Write-Host "Invalid choice. Please run the script again." -ForegroundColor Red
    }
}

Write-Host ""
Read-Host "Press Enter to continue"
