package io.reflectoring.demo.service;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQSession;
import org.apache.activemq.pool.PooledConnection;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Service
@EnableScheduling
public class ConnectionMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionMonitoringService.class);
    @Autowired
    private PooledConnectionFactory pooledConnectionFactory;

    @Autowired
    private ConnectionFactory connectionFactory; // This will be the CachingConnectionFactory    @Autowired
    private ActiveMQJmxMonitoringService jmxMonitoringService;
    
    @Autowired
    private ParallelProcessingMonitorService parallelProcessingMonitor;

    @Scheduled(fixedRate = 30000) // Log every 30 seconds
    public void logConnectionStats() {
        try {
            // Basic connection pool stats
            int numConnections = pooledConnectionFactory.getNumConnections();
            int maxConnections = pooledConnectionFactory.getMaxConnections();
            double usage = maxConnections > 0 ? (numConnections * 100.0 / maxConnections) : 0;

            // Get detailed connection information
            ConnectionInfo connInfo = getDetailedConnectionInfo();            // Get broker statistics via JMX
            ActiveMQJmxMonitoringService.BrokerStats brokerStats = jmxMonitoringService.getBrokerStats();
            
            // Get parallel processing statistics
            ParallelProcessingMonitorService.ParallelProcessingStats parallelStats = parallelProcessingMonitor.getStats();

            logger.info("=== JMS Connection Pool & Broker Statistics ===");
            logger.info("Client Pool - Active: {}, Max: {}, Usage: {:.1f}%",
                    numConnections, maxConnections, usage);
            logger.info("Client Sessions - Active: {}, Cached: {}, Max per Connection: {}",
                    connInfo.activeSessions, connInfo.cachedSessions, connInfo.maxSessionsPerConnection);
            logger.info("Client Consumers/Producers - Consumers: {} (cached: {}), Producers: {} (cached: {})",
                    connInfo.activeConsumers, connInfo.cachedConsumers, connInfo.activeProducers, connInfo.cachedProducers);
            logger.info("Parallel Processing - Active: {}, Peak: {}, Total Processed: {}, Avg Time: {}ms",
                    parallelStats.activeProcessingCount, parallelStats.peakConcurrentProcessing, 
                    parallelStats.totalProcessedMessages, parallelStats.averageProcessingTimeMs);

            if (brokerStats.isValid()) {
                logger.info("Broker Stats - Connections: {}, Total Consumers: {}, Total Messages: {}",
                        brokerStats.connectionCount, brokerStats.totalConsumerCount, brokerStats.totalMessageCount);
                logger.info("demo.queue - Messages: {}, Consumers: {}, Added: {}, Acknowledged: {}",
                        brokerStats.queueMessageCount, brokerStats.queueConsumerCount,
                        brokerStats.messagesAdded, brokerStats.messagesAcknowledged);
            } else {
                logger.info("Broker Stats - Not available (JMX connection failed)");
            }
            
            // Log active processing threads if any
            if (parallelStats.activeThreadDetails.length > 0) {
                logger.info("Active Processing Threads: {}", String.join(", ", parallelStats.activeThreadDetails));
            }
            
            logger.info("==============================================");

        } catch (Exception e) {
            logger.warn("Could not retrieve connection pool statistics: {}", e.getMessage());
        }
    }

    public void logCurrentStats() {
        try {
            int numConnections = pooledConnectionFactory.getNumConnections();
            int maxConnections = pooledConnectionFactory.getMaxConnections();

            ConnectionInfo connInfo = getDetailedConnectionInfo();

            logger.info("Current Connection Pool Stats - Active Connections: {}, Max: {}, Sessions: {}, Consumers: {}, Producers: {}",
                    numConnections, maxConnections, connInfo.activeSessions, connInfo.activeConsumers, connInfo.activeProducers);

        } catch (Exception e) {
            logger.warn("Could not retrieve connection pool statistics: {}", e.getMessage());
        }
    }

    private ConnectionInfo getDetailedConnectionInfo() {
        ConnectionInfo info = new ConnectionInfo();

        try {
            // Get information from the pooled connection factory
            info.maxSessionsPerConnection = pooledConnectionFactory.getMaximumActiveSessionPerConnection();

            // Try to get session information from CachingConnectionFactory if available
            if (connectionFactory instanceof CachingConnectionFactory) {
                CachingConnectionFactory cachingFactory = (CachingConnectionFactory) connectionFactory;
                info.cachedSessions = getCachedSessionCount(cachingFactory);
                info.cachedConsumers = getCachedConsumerCount(cachingFactory);
                info.cachedProducers = getCachedProducerCount(cachingFactory);
            }

            // Get active session information (this is an approximation)
            info.activeSessions = getActiveSessionCount();
            info.activeConsumers = getActiveConsumerCount();
            info.activeProducers = getActiveProducerCount();

        } catch (Exception e) {
            logger.debug("Error getting detailed connection info: {}", e.getMessage());
        }

        return info;
    }

    private int getCachedSessionCount(CachingConnectionFactory cachingFactory) {
        try {
            // Use reflection to access the session cache size
            Field sessionCacheSizeField = CachingConnectionFactory.class.getDeclaredField("sessionCacheSize");
            sessionCacheSizeField.setAccessible(true);
            return (Integer) sessionCacheSizeField.get(cachingFactory);
        } catch (Exception e) {
            return 0;
        }
    }

    private int getCachedConsumerCount(CachingConnectionFactory cachingFactory) {
        try {
            // Check if caching consumers is enabled
            Method isCacheConsumersMethod = CachingConnectionFactory.class.getDeclaredMethod("isCacheConsumers");
            isCacheConsumersMethod.setAccessible(true);
            boolean cacheConsumers = (Boolean) isCacheConsumersMethod.invoke(cachingFactory);
            return cacheConsumers ? 1 : 0; // Simplified - actual count would need deeper reflection
        } catch (Exception e) {
            return 0;
        }
    }

    private int getCachedProducerCount(CachingConnectionFactory cachingFactory) {
        try {
            // Check if caching producers is enabled
            Method isCacheProducersMethod = CachingConnectionFactory.class.getDeclaredMethod("isCacheProducers");
            isCacheProducersMethod.setAccessible(true);
            boolean cacheProducers = (Boolean) isCacheProducersMethod.invoke(cachingFactory);
            return cacheProducers ? 1 : 0; // Simplified - actual count would need deeper reflection
        } catch (Exception e) {
            return 0;
        }
    }    private int getActiveSessionCount() {
        try {
            return getActualActiveSessionCount();
        } catch (Exception e) {
            logger.debug("Could not get actual session count, using estimation: {}", e.getMessage());
            // Fallback to estimation
            int connections = pooledConnectionFactory.getNumConnections();
            return Math.min(connections * 2, pooledConnectionFactory.getMaximumActiveSessionPerConnection());
        }
    }
    
    private int getActualActiveSessionCount() throws Exception {
        int sessionCount = 0;
        
        // Get the connection pool from PooledConnectionFactory
        Field connectionsField = PooledConnectionFactory.class.getDeclaredField("connections");
        connectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Object, Object> connections = (Map<Object, Object>) connectionsField.get(pooledConnectionFactory);
        
        if (connections != null) {
            for (Object connectionWrapper : connections.values()) {
                sessionCount += getSessionCountFromConnection(connectionWrapper);
            }
        }
        
        return sessionCount;
    }
    
    private int getSessionCountFromConnection(Object connectionWrapper) {
        try {
            // Try to get the actual ActiveMQ connection
            Field connectionField = connectionWrapper.getClass().getDeclaredField("connection");
            connectionField.setAccessible(true);
            Object connection = connectionField.get(connectionWrapper);
            
            if (connection instanceof ActiveMQConnection) {
                ActiveMQConnection amqConnection = (ActiveMQConnection) connection;
                
                // Get sessions from ActiveMQ connection
                Field sessionsField = ActiveMQConnection.class.getDeclaredField("sessions");
                sessionsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<Object, Object> sessions = (Map<Object, Object>) sessionsField.get(amqConnection);
                
                return sessions != null ? sessions.size() : 0;
            }
        } catch (Exception e) {
            logger.debug("Could not get session count from connection: {}", e.getMessage());
        }
        return 0;
    }

    private int getActiveConsumerCount() {
        try {
            return getActualActiveConsumerCount();
        } catch (Exception e) {
            logger.debug("Could not get actual consumer count, using estimation: {}", e.getMessage());
            // Fallback to JMS listener concurrency setting
            return 5;
        }
    }
    
    private int getActualActiveConsumerCount() throws Exception {
        int consumerCount = 0;
        
        // Get the connection pool from PooledConnectionFactory
        Field connectionsField = PooledConnectionFactory.class.getDeclaredField("connections");
        connectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Object, Object> connections = (Map<Object, Object>) connectionsField.get(pooledConnectionFactory);
        
        if (connections != null) {
            for (Object connectionWrapper : connections.values()) {
                consumerCount += getConsumerCountFromConnection(connectionWrapper);
            }
        }
        
        return consumerCount;
    }
    
    private int getConsumerCountFromConnection(Object connectionWrapper) {
        try {
            // Try to get the actual ActiveMQ connection
            Field connectionField = connectionWrapper.getClass().getDeclaredField("connection");
            connectionField.setAccessible(true);
            Object connection = connectionField.get(connectionWrapper);
            
            if (connection instanceof ActiveMQConnection) {
                ActiveMQConnection amqConnection = (ActiveMQConnection) connection;
                
                // Get sessions and count consumers in each session
                Field sessionsField = ActiveMQConnection.class.getDeclaredField("sessions");
                sessionsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<Object, Object> sessions = (Map<Object, Object>) sessionsField.get(amqConnection);
                
                int consumers = 0;
                if (sessions != null) {
                    for (Object session : sessions.values()) {
                        if (session instanceof ActiveMQSession) {
                            ActiveMQSession amqSession = (ActiveMQSession) session;
                            try {
                                Field consumersField = ActiveMQSession.class.getDeclaredField("consumers");
                                consumersField.setAccessible(true);
                                @SuppressWarnings("unchecked")
                                Map<Object, Object> sessionConsumers = (Map<Object, Object>) consumersField.get(amqSession);
                                if (sessionConsumers != null) {
                                    consumers += sessionConsumers.size();
                                }
                            } catch (Exception e) {
                                logger.debug("Could not get consumers from session: {}", e.getMessage());
                            }
                        }
                    }
                }
                return consumers;
            }
        } catch (Exception e) {
            logger.debug("Could not get consumer count from connection: {}", e.getMessage());
        }
        return 0;
    }

    private int getActiveProducerCount() {
        try {
            return getActualActiveProducerCount();
        } catch (Exception e) {
            logger.debug("Could not get actual producer count, using estimation: {}", e.getMessage());
            // Fallback to estimation
            return 1;
        }
    }
    
    private int getActualActiveProducerCount() throws Exception {
        int producerCount = 0;
        
        // Get the connection pool from PooledConnectionFactory
        Field connectionsField = PooledConnectionFactory.class.getDeclaredField("connections");
        connectionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Object, Object> connections = (Map<Object, Object>) connectionsField.get(pooledConnectionFactory);
        
        if (connections != null) {
            for (Object connectionWrapper : connections.values()) {
                producerCount += getProducerCountFromConnection(connectionWrapper);
            }
        }
        
        return producerCount;
    }
    
    private int getProducerCountFromConnection(Object connectionWrapper) {
        try {
            // Try to get the actual ActiveMQ connection
            Field connectionField = connectionWrapper.getClass().getDeclaredField("connection");
            connectionField.setAccessible(true);
            Object connection = connectionField.get(connectionWrapper);
            
            if (connection instanceof ActiveMQConnection) {
                ActiveMQConnection amqConnection = (ActiveMQConnection) connection;
                
                // Get sessions and count producers in each session
                Field sessionsField = ActiveMQConnection.class.getDeclaredField("sessions");
                sessionsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<Object, Object> sessions = (Map<Object, Object>) sessionsField.get(amqConnection);
                
                int producers = 0;
                if (sessions != null) {
                    for (Object session : sessions.values()) {
                        if (session instanceof ActiveMQSession) {
                            ActiveMQSession amqSession = (ActiveMQSession) session;
                            try {
                                Field producersField = ActiveMQSession.class.getDeclaredField("producers");
                                producersField.setAccessible(true);
                                @SuppressWarnings("unchecked")
                                Map<Object, Object> sessionProducers = (Map<Object, Object>) producersField.get(amqSession);
                                if (sessionProducers != null) {
                                    producers += sessionProducers.size();
                                }
                            } catch (Exception e) {
                                logger.debug("Could not get producers from session: {}", e.getMessage());
                            }
                        }
                    }
                }
                return producers;
            }
        } catch (Exception e) {
            logger.debug("Could not get producer count from connection: {}", e.getMessage());
        }
        return 0;
    }

    private static class ConnectionInfo {
        int activeSessions = 0;
        int cachedSessions = 0;
        int maxSessionsPerConnection = 0;
        int activeConsumers = 0;
        int cachedConsumers = 0;
        int activeProducers = 0;
        int cachedProducers = 0;
    }
}
