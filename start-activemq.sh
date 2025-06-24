#!/bin/bash

echo "🚀 Starting ActiveMQ with Docker..."

# Start only ActiveMQ
docker-compose up -d activemq

echo "⏳ Waiting for ActiveMQ to be ready..."
sleep 10

# Check if ActiveMQ is running
if docker-compose ps activemq | grep -q "Up"; then
    echo "✅ ActiveMQ is running!"
    echo ""
    echo "📊 ActiveMQ Web Console: http://localhost:8161/console/"
    echo "👤 Username: admin"
    echo "🔑 Password: admin"
    echo ""
    echo "🔌 JMS Connection URL: tcp://localhost:61616"
    echo ""
    echo "🧪 Test your Spring Boot app by running: mvn spring-boot:run"
    echo "📨 Then call: curl http://localhost:8080/api/messages/publish"
else
    echo "❌ Failed to start ActiveMQ"
    docker-compose logs activemq
fi
