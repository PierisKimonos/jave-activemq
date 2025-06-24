# 🚀 Quick Start Guide

## Option 1: With Full Monitoring (Recommended)

```bash
# Windows
.\start-monitoring.bat

# Linux/Mac
./start-monitoring.sh
```

Then start your Spring Boot app:

```bash
mvn spring-boot:run
```

Access dashboards:

- **Grafana**: http://localhost:3000 (admin/admin)
- **ActiveMQ Console**: http://localhost:8161/console/ (admin/admin)

## Option 2: Basic Setup (No Monitoring)

```bash
# Windows
.\start-activemq.bat

# Linux/Mac
./start-activemq.sh
```

Then start your Spring Boot app:

```bash
mvn spring-boot:run
```

Test it:

```bash
# Windows
.\test-service.bat

# Linux/Mac
./test-service.sh
```

## Option 2: Step-by-Step

1. **Start ActiveMQ in Docker**:

   ```bash
   docker-compose up -d activemq
   ```

2. **Start Spring Boot App**:

   ```bash
   mvn spring-boot:run
   ```

3. **Test the endpoints**:

   ```bash
   # Publish 1000 messages
   curl http://localhost:8080/api/messages/publish

   # Check status
   curl http://localhost:8080/api/messages/status
   ```

## 📊 Monitoring

- **ActiveMQ Console**: http://localhost:8161/console/ (admin/admin)
- **App Health**: http://localhost:8080/api/messages/health
- **Message Status**: http://localhost:8080/api/messages/status

## 🧹 Cleanup

```bash
docker-compose down -v
```

## 🔧 Advanced

Run both ActiveMQ and Spring Boot in Docker:

```bash
docker-compose --profile app up -d
```
