package io.reflectoring.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Service to monitor ActiveMQ broker statistics via JMX
 */
@Service
public class ActiveMQJmxMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(ActiveMQJmxMonitoringService.class);

    @Value("${activemq.jmx.url:service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi}")
    private String jmxUrl;

    @Value("${activemq.jmx.username:admin}")
    private String jmxUsername;

    @Value("${activemq.jmx.password:admin}")
    private String jmxPassword;

    public BrokerStats getBrokerStats() {
        JMXConnector connector = null;
        try {
            // Connect to ActiveMQ JMX
            Map<String, Object> environment = new HashMap<>();
            if (jmxUsername != null && !jmxUsername.isEmpty()) {
                String[] credentials = {jmxUsername, jmxPassword};
                environment.put(JMXConnector.CREDENTIALS, credentials);
            }

            JMXServiceURL serviceURL = new JMXServiceURL(jmxUrl);
            connector = JMXConnectorFactory.connect(serviceURL, environment);
            MBeanServerConnection mbsc = connector.getMBeanServerConnection();

            BrokerStats stats = new BrokerStats();

            // Get broker information
            ObjectName brokerName = new ObjectName("org.apache.activemq.artemis:broker=\"*\"");
            Set<ObjectName> brokerObjects = mbsc.queryNames(brokerName, null);
            
            if (!brokerObjects.isEmpty()) {
                ObjectName broker = brokerObjects.iterator().next();
                
                // Get connection count
                try {
                    stats.connectionCount = (Long) mbsc.getAttribute(broker, "ConnectionCount");
                } catch (Exception e) {
                    logger.debug("Could not get connection count: {}", e.getMessage());
                }

                // Get total message count
                try {
                    stats.totalMessageCount = (Long) mbsc.getAttribute(broker, "TotalMessageCount");
                } catch (Exception e) {
                    logger.debug("Could not get total message count: {}", e.getMessage());
                }

                // Get consumer count
                try {
                    stats.totalConsumerCount = (Long) mbsc.getAttribute(broker, "TotalConsumerCount");
                } catch (Exception e) {
                    logger.debug("Could not get consumer count: {}", e.getMessage());
                }
            }

            // Get queue-specific statistics
            ObjectName queueName = new ObjectName("org.apache.activemq.artemis:broker=\"*\",component=addresses,address=\"demo.queue\",subcomponent=queues,routing-type=\"anycast\",queue=\"demo.queue\"");
            Set<ObjectName> queueObjects = mbsc.queryNames(queueName, null);
            
            if (!queueObjects.isEmpty()) {
                ObjectName queue = queueObjects.iterator().next();
                
                try {
                    stats.queueMessageCount = (Long) mbsc.getAttribute(queue, "MessageCount");
                    stats.queueConsumerCount = (Long) mbsc.getAttribute(queue, "ConsumerCount");
                    stats.messagesAdded = (Long) mbsc.getAttribute(queue, "MessagesAdded");
                    stats.messagesAcknowledged = (Long) mbsc.getAttribute(queue, "MessagesAcknowledged");
                } catch (Exception e) {
                    logger.debug("Could not get queue statistics: {}", e.getMessage());
                }
            }

            return stats;

        } catch (Exception e) {
            logger.warn("Could not connect to ActiveMQ JMX: {}", e.getMessage());
            return new BrokerStats(); // Return empty stats
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (Exception e) {
                    logger.debug("Error closing JMX connector: {}", e.getMessage());
                }
            }
        }
    }

    public void logBrokerStats() {
        BrokerStats stats = getBrokerStats();
        
        if (stats.isValid()) {
            logger.info("=== ActiveMQ Broker Statistics ===");
            logger.info("Broker Connections: {}", stats.connectionCount);
            logger.info("Total Consumers: {}", stats.totalConsumerCount);
            logger.info("Total Messages: {}", stats.totalMessageCount);
            logger.info("--- demo.queue Statistics ---");
            logger.info("Queue Messages: {}, Consumers: {}", stats.queueMessageCount, stats.queueConsumerCount);
            logger.info("Messages Added: {}, Acknowledged: {}", stats.messagesAdded, stats.messagesAcknowledged);
            logger.info("=================================");
        } else {
            logger.debug("Broker statistics not available (JMX connection failed)");
        }
    }

    public static class BrokerStats {
        public long connectionCount = 0;
        public long totalConsumerCount = 0;
        public long totalMessageCount = 0;
        public long queueMessageCount = 0;
        public long queueConsumerCount = 0;
        public long messagesAdded = 0;
        public long messagesAcknowledged = 0;

        public boolean isValid() {
            return connectionCount > 0 || totalConsumerCount > 0 || queueMessageCount > 0;
        }
    }
}
