#!/bin/bash

echo "🚀 Starting Demo MQ Service with Monitoring..."

echo "📊 Starting monitoring stack (Prometheus + Grafana)..."
docker-compose --profile monitoring up -d

echo "⏳ Waiting for monitoring services to be ready..."
sleep 15

echo "🔧 Starting ActiveMQ..."
docker-compose up -d activemq

echo "⏳ Waiting for ActiveMQ to be ready..."
sleep 10

# Check if services are running
if docker-compose ps | grep -q "Up"; then
    echo "✅ All services are running!"
    echo ""
    echo "📊 Monitoring URLs:"
    echo "   - Grafana Dashboard: http://localhost:3000 (admin/admin)"
    echo "   - Prometheus: http://localhost:9090"
    echo "   - ActiveMQ Console: http://localhost:8161/console/ (admin/admin)"
    echo ""
    echo "🔌 JMS Connection URL: tcp://localhost:61616"
    echo ""
    echo "🧪 Next steps:"
    echo "   1. Start your Spring Boot app: mvn spring-boot:run"
    echo "   2. Test the service: ./test-service.sh"
    echo "   3. Monitor in Grafana: http://localhost:3000"
else
    echo "❌ Failed to start services"
    docker-compose logs
fi
