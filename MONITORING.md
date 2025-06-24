# 📊 Monitoring Guide

## Quick Start with Full Monitoring

### Option 1: Complete Monitoring Stack

```bash
# Windows
.\start-monitoring.bat

# Linux/Mac
chmod +x start-monitoring.sh
./start-monitoring.sh
```

### Option 2: Step by Step

```bash
# Start monitoring services
docker-compose --profile monitoring up -d

# Start ActiveMQ
docker-compose up -d activemq

# Start your Spring Boot app
mvn spring-boot:run

# Test the service
curl http://localhost:8080/api/messages/publish
```

## 🖥️ Access Points

### Dashboards & Monitoring

- **Grafana**: http://localhost:3000 (admin/admin)
  - Spring Boot Application Dashboard
  - ActiveMQ Queue Dashboard
- **Prometheus**: http://localhost:9090
- **ActiveMQ Console**: http://localhost:8161/console/ (admin/admin)

### Application

- **Service API**: http://localhost:8080/api/messages/publish
- **Health Check**: http://localhost:8080/api/messages/health
- **Metrics Endpoint**: http://localhost:8080/actuator/prometheus

## 📈 Available Metrics

### Spring Boot Application Metrics

- **Messages Published**: Total and rate of messages published
- **Messages Consumed**: Total and rate of messages consumed
- **Processing Time**: Average message processing duration
- **Error Rates**: Publishing and processing error rates
- **JVM Metrics**: Memory usage, GC, threads
- **HTTP Metrics**: Request rates, response times

### ActiveMQ Queue Metrics

- **Queue Depth**: Number of messages in queue
- **Throughput**: Messages enqueued/dequeued per second
- **Consumer Count**: Number of active consumers
- **Broker Memory**: Memory usage percentage
- **Connection Count**: Active connections to broker

## 🎯 Key Dashboard Features

### Application Dashboard

- Real-time message flow visualization
- Processing performance metrics
- Error rate monitoring
- JVM health indicators
- HTTP request tracking

### ActiveMQ Dashboard

- Queue depth monitoring
- Message throughput graphs
- Consumer activity tracking
- Broker resource utilization
- Connection management

## 🧪 Testing Scenarios

### 1. Load Testing

```bash
# Publish messages and watch metrics
curl http://localhost:8080/api/messages/publish

# Monitor in Grafana:
# - Message rates should spike
# - Queue depth should increase then decrease
# - Processing time should remain stable
# - 5 consumers should be active
```

### 2. Performance Monitoring

```bash
# Check current status
curl http://localhost:8080/api/messages/status

# Reset counters for clean test
curl -X POST http://localhost:8080/api/messages/reset

# Watch metrics in Grafana during processing
```

### 3. Error Simulation

```bash
# Stop ActiveMQ to simulate connection issues
docker-compose stop activemq

# Try to publish (will fail)
curl http://localhost:8080/api/messages/publish

# Check error metrics in Grafana
# Restart ActiveMQ
docker-compose start activemq
```

## 🔧 Configuration

### Metrics Collection Interval

- **Prometheus scrape interval**: 15 seconds
- **Grafana refresh**: 10 seconds
- **Application metrics**: Real-time

### Data Retention

- **Prometheus**: 200 hours
- **Grafana**: Persistent dashboards

## 🧹 Cleanup

```bash
# Stop all services
docker-compose --profile monitoring down

# Remove volumes (clean slate)
docker-compose --profile monitoring down -v
```

## 🚨 Alerts (Future Enhancement)

You can configure alerts in Grafana for:

- Queue depth > 1000 messages
- Error rate > 5%
- Memory usage > 80%
- No active consumers
- Processing time > 500ms

## 📝 Troubleshooting

### Metrics Not Showing

1. Ensure Spring Boot app is running with Actuator
2. Check Prometheus targets: http://localhost:9090/targets
3. Verify metrics endpoint: http://localhost:8080/actuator/prometheus

### Dashboard Issues

1. Check Grafana data source configuration
2. Verify Prometheus connectivity
3. Import dashboards manually if needed

### Container Issues

```bash
# Check container status
docker-compose ps

# View logs
docker-compose logs grafana
docker-compose logs prometheus
docker-compose logs activemq
```
