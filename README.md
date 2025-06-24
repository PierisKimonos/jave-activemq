# Demo MQ Service

This Spring Boot application provides a messaging service that publishes 1000 messages to an MQ queue and consumes them with a concurrency of 5 listeners.

## Features

- **GET Endpoint**: `/api/messages/publish` - Publishes 1000 messages to the queue
- **Message Consumer**: Listens to messages with concurrency level 5
- **Connection Management**: Bounded connection pool to respect MQ server limits
- **Connection Monitoring**: Logs connection pool statistics every 30 seconds
- **Proper Cleanup**: Ensures all connections are released on shutdown

## Configuration

### Connection Pool Settings (application.properties)

```properties
# Maximum number of connections to the MQ broker
spring.activemq.pool.max-connections=10

# Maximum sessions per connection
spring.activemq.pool.maximum-active-session-per-connection=5

# Listener concurrency (5 concurrent consumers)
spring.jms.listener.concurrency=5
spring.jms.listener.max-concurrency=5
```

### Important Configuration Parameters

- **max-connections**: Controls the maximum number of connections your service will create. Adjust this based on your MQ server's connection limit.
- **maximum-active-session-per-connection**: Controls how many sessions each connection can have.
- **concurrency**: Fixed at 5 as per requirements.

## API Endpoints

### 1. Publish Messages

```bash
GET /api/messages/publish
```

Starts publishing 1000 messages asynchronously to the queue.

**Response:**

```json
{
  "status": "STARTED",
  "message": "Publishing 1000 messages started",
  "timestamp": 1703357234567
}
```

### 2. Check Status

```bash
GET /api/messages/status
```

Returns current message processing statistics.

**Response:**

```json
{
  "publishedMessages": 1000,
  "consumedMessages": 856,
  "processedMessages": 856,
  "timestamp": 1703357234567
}
```

### 3. Reset Counters

```bash
POST /api/messages/reset
```

Resets message counters for testing purposes.

### 4. Health Check

```bash
GET /api/messages/health
```

Basic health check endpoint.

## Running the Application

### Quick Start with Docker

The easiest way to test this service is using Docker for ActiveMQ:

#### Option 1: Using the provided scripts (Recommended)

**Windows:**

```bash
# Start ActiveMQ
.\start-activemq.bat

# In another terminal, start your Spring Boot app
mvn spring-boot:run

# Test the service
.\test-service.bat
```

**Linux/Mac:**

```bash
# Start ActiveMQ
chmod +x start-activemq.sh
./start-activemq.sh

# In another terminal, start your Spring Boot app
mvn spring-boot:run

# Test the service
chmod +x test-service.sh
./test-service.sh
```

#### Option 2: Manual Docker commands

```bash
# Start ActiveMQ only
docker-compose up -d activemq

# Or start everything (ActiveMQ + Spring Boot app)
docker-compose --profile app up -d
```

### Traditional Setup

#### Prerequisites

1. Java 17 or later
2. Maven 3.6 or later
3. ActiveMQ broker running (default: localhost:61616)

### Steps

1. **Start ActiveMQ Broker**:

   - Download and start ActiveMQ
   - Default URL: http://localhost:8161/admin (admin/admin)

2. **Update Configuration**:

   - Modify `application.properties` to match your MQ broker settings
   - Adjust `max-connections` based on your broker's limits

3. **Build and Run**:

   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

4. **Test the Service**:

   ```bash
   # Publish 1000 messages
   curl http://localhost:8080/api/messages/publish

   # Check processing status
   curl http://localhost:8080/api/messages/status
   ```

## Docker Configuration

### ActiveMQ Access

When ActiveMQ is running in Docker:

- **Web Console**: http://localhost:8161/console/
- **Username/Password**: admin/admin
- **JMS Connection**: tcp://localhost:61616

### Container Management

```bash
# View running containers
docker-compose ps

# View ActiveMQ logs
docker-compose logs activemq

# Stop containers
docker-compose down

# Cleanup (removes volumes too)
docker-compose down -v
```

### Environment Variables

The Spring Boot app supports these environment variables:

- `SPRING_ACTIVEMQ_BROKER_URL`: ActiveMQ broker URL (default: tcp://localhost:61616)
- `SPRING_ACTIVEMQ_USER`: ActiveMQ username (default: admin)
- `SPRING_ACTIVEMQ_PASSWORD`: ActiveMQ password (default: admin)

## Connection Management Features

### 1. Bounded Connection Pool

- Maximum connections are configured via `spring.activemq.pool.max-connections`
- Prevents exceeding your MQ server's connection limits
- Uses Apache ActiveMQ's PooledConnectionFactory

### 2. Session Management

- Sessions are cached and reused efficiently
- Maximum sessions per connection controlled by configuration
- Automatic session cleanup

### 3. Connection Monitoring

- Logs connection pool statistics every 30 seconds
- Helps monitor connection usage and prevent leaks
- Available in application logs

### 4. Graceful Shutdown

- Proper cleanup of all connections on application shutdown
- Prevents connection leaks
- Ensures all resources are released

## Monitoring and Logging

The application logs important information:

- Message publishing progress (every 100 messages)
- Message consumption progress (every 100 messages)
- Connection pool statistics (every 30 seconds)
- Thread information for concurrent processing

## Configuration Best Practices

1. **Set max-connections** based on your MQ server's limits
2. **Monitor connection usage** through logs
3. **Adjust session limits** based on your message volume
4. **Use connection pooling** to optimize performance
5. **Implement proper shutdown** to prevent connection leaks

## Example Configuration for Different Scenarios

### High Throughput (More Connections)

```properties
spring.activemq.pool.max-connections=20
spring.activemq.pool.maximum-active-session-per-connection=200
```

### Conservative (Fewer Connections)

```properties
spring.activemq.pool.max-connections=5
spring.activemq.pool.maximum-active-session-per-connection=50
```

## Troubleshooting

1. **Connection Pool Exhausted**: Increase `max-connections` or decrease message volume
2. **MQ Server Connection Limit**: Reduce `max-connections` in configuration
3. **Slow Processing**: Monitor logs for processing times and adjust concurrency
4. **Connection Leaks**: Check shutdown logs for proper cleanup
