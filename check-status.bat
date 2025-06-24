@echo off
echo 🔍 Checking Service Status...
echo.

echo 📋 Container Status:
docker-compose ps

echo.
echo 🌐 Testing ActiveMQ Console...
curl.exe -s -o nul -w "ActiveMQ Console: %%{http_code}\n" http://localhost:8161/console/

echo.
echo 📊 Testing Grafana (if running)...
curl.exe -s -o nul -w "Grafana: %%{http_code}\n" http://localhost:3000/

echo.
echo 🔧 Testing Prometheus (if running)...
curl.exe -s -o nul -w "Prometheus: %%{http_code}\n" http://localhost:9090/

echo.
echo 💡 Status codes: 200/302 = OK, 000 = Not running/ready yet
