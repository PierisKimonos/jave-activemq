#!/bin/bash

echo "🧪 Testing the MQ Demo Service..."

BASE_URL="http://localhost:8080"

echo "1️⃣ Health Check..."
curl -s "$BASE_URL/api/messages/health" | python -m json.tool
echo ""

echo "2️⃣ Publishing 1000 messages..."
curl -s "$BASE_URL/api/messages/publish" | python -m json.tool
echo ""

echo "3️⃣ Waiting 5 seconds..."
sleep 5

echo "4️⃣ Checking status..."
curl -s "$BASE_URL/api/messages/status" | python -m json.tool
echo ""

echo "5️⃣ Waiting another 10 seconds for processing..."
sleep 10

echo "6️⃣ Final status check..."
curl -s "$BASE_URL/api/messages/status" | python -m json.tool
echo ""

echo "✅ Test completed!"
echo "🖥️  Check ActiveMQ console at: http://localhost:8161/console/"
