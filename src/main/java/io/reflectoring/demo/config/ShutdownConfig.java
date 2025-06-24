package io.reflectoring.demo.config;

import io.reflectoring.demo.service.MessageProducerService;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;

@Configuration
public class ShutdownConfig {

    private static final Logger logger = LoggerFactory.getLogger(ShutdownConfig.class);

    @Autowired
    private PooledConnectionFactory pooledConnectionFactory;

    @Autowired
    private MessageProducerService messageProducerService;

    @PreDestroy
    public void cleanupConnections() {
        logger.info("Application shutting down - cleaning up connections");
        
        try {
            // Shutdown message producer service
            messageProducerService.shutdown();
            logger.info("Message producer service shutdown completed");
            
            // Stop the connection factory
            if (pooledConnectionFactory != null) {
                pooledConnectionFactory.stop();
                logger.info("Connection factory stopped - all connections should be released");
            }
        } catch (Exception e) {
            logger.error("Error during connection cleanup", e);
        }
    }
}
