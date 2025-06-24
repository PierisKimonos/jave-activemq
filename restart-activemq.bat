@echo off
echo Restarting ActiveMQ Artemis...

echo Stopping ActiveMQ container...
docker-compose stop activemq

echo Removing ActiveMQ container...
docker-compose rm -f activemq

echo Starting ActiveMQ container...
docker-compose up activemq -d

echo Waiting for ActiveMQ to start...
timeout /t 10 /nobreak > nul

echo Checking ActiveMQ status...
docker-compose ps activemq

echo Done! ActiveMQ should be running now.
echo Web console available at: http://localhost:8161/console
echo Username: admin
echo Password: admin
