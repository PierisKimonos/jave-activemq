@echo off
echo 🧪 Testing the MQ Demo Service...

set BASE_URL=http://localhost:8080

echo 1️⃣ Health Check...
curl -s "%BASE_URL%/api/messages/health"
echo.

echo 2️⃣ Publishing 1000 messages...
curl -s "%BASE_URL%/api/messages/publish"
echo.

echo 3️⃣ Waiting 5 seconds...
timeout /t 5 /nobreak >nul

echo 4️⃣ Checking status...
curl -s "%BASE_URL%/api/messages/status"
echo.

echo 5️⃣ Waiting another 10 seconds for processing...
timeout /t 10 /nobreak >nul

echo 6️⃣ Final status check...
curl -s "%BASE_URL%/api/messages/status"
echo.

echo ✅ Test completed!
echo 🖥️  Check ActiveMQ console at: http://localhost:8161/console/
