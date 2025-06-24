package io.reflectoring.demo.controller;

import io.reflectoring.demo.service.ActiveMQJmxMonitoringService;
import io.reflectoring.demo.service.ConnectionMonitoringService;
import io.reflectoring.demo.service.MessageConsumerService;
import io.reflectoring.demo.service.ParallelProcessingMonitorService;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    @Autowired
    private ConnectionMonitoringService connectionMonitoringService;
    @Autowired
    private ActiveMQJmxMonitoringService jmxMonitoringService;

    @Autowired
    private PooledConnectionFactory pooledConnectionFactory;
    @Autowired
    private ParallelProcessingMonitorService parallelProcessingMonitor;

    @Autowired
    private MessageConsumerService messageConsumerService;

    @GetMapping("/connection-stats")
    public Map<String, Object> getConnectionStats() {
        Map<String, Object> stats = new HashMap<>();

        // Connection pool stats
        stats.put("activeConnections", pooledConnectionFactory.getNumConnections());
        stats.put("maxConnections", pooledConnectionFactory.getMaxConnections());
        stats.put("maxSessionsPerConnection", pooledConnectionFactory.getMaximumActiveSessionPerConnection());
        stats.put("idleTimeout", pooledConnectionFactory.getIdleTimeout());

        double usage = pooledConnectionFactory.getMaxConnections() > 0
                ? (pooledConnectionFactory.getNumConnections() * 100.0 / pooledConnectionFactory.getMaxConnections())
                : 0;
        stats.put("usagePercentage", Math.round(usage * 10.0) / 10.0);

        return stats;
    }

    @GetMapping("/broker-stats")
    public ActiveMQJmxMonitoringService.BrokerStats getBrokerStats() {
        return jmxMonitoringService.getBrokerStats();
    }

    @GetMapping("/all-stats")
    public Map<String, Object> getAllStats() {
        Map<String, Object> allStats = new HashMap<>();

        // Connection pool stats
        allStats.put("connectionPool", getConnectionStats());

        // Session details with actual counts
        allStats.put("sessionDetails", getSessionDetails());

        // JMS Configuration including concurrency settings
        allStats.put("jmsConfig", getJmsConfig());

        // Parallel processing stats
        allStats.put("parallelProcessing", getParallelProcessingStats());

        // Broker stats
        ActiveMQJmxMonitoringService.BrokerStats brokerStats = jmxMonitoringService.getBrokerStats();
        Map<String, Object> brokerStatsMap = new HashMap<>();
        brokerStatsMap.put("connectionCount", brokerStats.connectionCount);
        brokerStatsMap.put("totalConsumerCount", brokerStats.totalConsumerCount);
        brokerStatsMap.put("totalMessageCount", brokerStats.totalMessageCount);
        brokerStatsMap.put("queueMessageCount", brokerStats.queueMessageCount);
        brokerStatsMap.put("queueConsumerCount", brokerStats.queueConsumerCount);
        brokerStatsMap.put("messagesAdded", brokerStats.messagesAdded);
        brokerStatsMap.put("messagesAcknowledged", brokerStats.messagesAcknowledged);
        brokerStatsMap.put("isValid", brokerStats.isValid());

        allStats.put("broker", brokerStatsMap);

        return allStats;
    }

    @GetMapping("/log-stats")
    public Map<String, String> logCurrentStats() {
        connectionMonitoringService.logCurrentStats();
        jmxMonitoringService.logBrokerStats();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Current statistics logged to console");
        return response;
    }

    @GetMapping("/session-details")
    public Map<String, Object> getSessionDetails() {
        Map<String, Object> details = new HashMap<>();

        try {
            // Get the detailed connection info which now includes actual session counts
            java.lang.reflect.Method getDetailedConnectionInfoMethod =
                    connectionMonitoringService.getClass().getDeclaredMethod("getDetailedConnectionInfo");
            getDetailedConnectionInfoMethod.setAccessible(true);
            Object connectionInfo = getDetailedConnectionInfoMethod.invoke(connectionMonitoringService);

            // Extract values using reflection
            java.lang.reflect.Field activeSessionsField = connectionInfo.getClass().getDeclaredField("activeSessions");
            activeSessionsField.setAccessible(true);
            int activeSessions = activeSessionsField.getInt(connectionInfo);

            java.lang.reflect.Field activeConsumersField = connectionInfo.getClass().getDeclaredField("activeConsumers");
            activeConsumersField.setAccessible(true);
            int activeConsumers = activeConsumersField.getInt(connectionInfo);

            java.lang.reflect.Field activeProducersField = connectionInfo.getClass().getDeclaredField("activeProducers");
            activeProducersField.setAccessible(true);
            int activeProducers = activeProducersField.getInt(connectionInfo);

            java.lang.reflect.Field cachedSessionsField = connectionInfo.getClass().getDeclaredField("cachedSessions");
            cachedSessionsField.setAccessible(true);
            int cachedSessions = cachedSessionsField.getInt(connectionInfo);

            java.lang.reflect.Field maxSessionsPerConnectionField = connectionInfo.getClass().getDeclaredField("maxSessionsPerConnection");
            maxSessionsPerConnectionField.setAccessible(true);
            int maxSessionsPerConnection = maxSessionsPerConnectionField.getInt(connectionInfo);

            details.put("activeSessions", activeSessions);
            details.put("cachedSessions", cachedSessions);
            details.put("maxSessionsPerConnection", maxSessionsPerConnection);
            details.put("activeConsumers", activeConsumers);
            details.put("activeProducers", activeProducers);
            details.put("timestamp", System.currentTimeMillis());
            details.put("note", "These are actual counts from the connection pool");

        } catch (Exception e) {
            details.put("error", "Could not retrieve session details: " + e.getMessage());
            details.put("activeSessions", "N/A");
            details.put("activeConsumers", "N/A");
            details.put("activeProducers", "N/A");
        }

        return details;
    }

    @GetMapping("/parallel-processing")
    public ParallelProcessingMonitorService.ParallelProcessingStats getParallelProcessingStats() {
        return parallelProcessingMonitor.getStats();
    }

    @GetMapping("/parallel-processing/reset-peak")
    public Map<String, String> resetPeakStats() {
        parallelProcessingMonitor.resetPeakStats();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Peak parallel processing statistics reset");
        return response;
    }

    @GetMapping("/parallel-processing/reset-all")
    public Map<String, String> resetAllParallelStats() {
        parallelProcessingMonitor.resetAllStats();
        Map<String, String> response = new HashMap<>();
        response.put("message", "All parallel processing statistics reset");
        return response;
    }

    @GetMapping("/jms-config")
    public Map<String, Object> getJmsConfig() {
        Map<String, Object> config = new HashMap<>();

        // JMS Listener Configuration
        config.put("concurrencyRange", messageConsumerService.getConcurrencyRange());
        config.put("minConcurrency", messageConsumerService.getMinConcurrency());
        config.put("maxConcurrency", messageConsumerService.getMaxConcurrency());

        // Message processing stats
        config.put("consumedMessageCount", messageConsumerService.getConsumedMessageCount());
        config.put("processedMessageCount", messageConsumerService.getProcessedMessageCount());

        return config;
    }
}
