#!/bin/bash

echo "🔧 Setting up Demo MQ Service..."

# Make scripts executable
chmod +x start-activemq.sh
chmod +x test-service.sh

echo "✅ Scripts are now executable!"
echo ""
echo "🚀 Quick Start:"
echo "1. Run: ./start-activemq.sh"
echo "2. Run: mvn spring-boot:run (in another terminal)"
echo "3. Run: ./test-service.sh (to test)"
echo ""
echo "📊 Monitor ActiveMQ at: http://localhost:8161/console/ (admin/admin)"
