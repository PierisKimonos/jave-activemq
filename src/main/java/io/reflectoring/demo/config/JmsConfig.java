package io.reflectoring.demo.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import jakarta.jms.ConnectionFactory;

@Configuration
@EnableJms
public class JmsConfig {

    @Value("${spring.activemq.broker-url}")
    private String brokerUrl;

    @Value("${spring.activemq.user}")
    private String username;

    @Value("${spring.activemq.password}")
    private String password;

    @Value("${spring.activemq.pool.max-connections}")
    private int maxConnections;

    @Value("${spring.activemq.pool.maximum-active-session-per-connection}")
    private int maxActiveSessionsPerConnection;

    @Value("${spring.jms.listener.min-concurrency}")
    private int minConcurrency;

    @Value("${spring.jms.listener.max-concurrency}")
    private int maxConcurrency;

    @Bean
    public ActiveMQConnectionFactory activeMQConnectionFactory() {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
        factory.setBrokerURL(brokerUrl);
        factory.setUserName(username);
        factory.setPassword(password);        factory.setTrustAllPackages(true);

        // Connection optimization settings to prevent timeouts
        factory.setOptimizeAcknowledge(false); // Disable to reduce log noise
        factory.setUseAsyncSend(false); // Use sync send for reliability
        factory.setOptimizedMessageDispatch(true);
        factory.setCopyMessageOnSend(false);
        factory.setDisableTimeStampsByDefault(false);
        // factory.setOptimizedAckScheduledAckInterval(1000); // Not needed when optimizeAcknowledge is false

        // Connection timeout settings to prevent connection drops
        factory.setCloseTimeout(15000);
        factory.setSendTimeout(10000);

        return factory;
    }

    @Bean
    public PooledConnectionFactory pooledConnectionFactory() {
        PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
        pooledConnectionFactory.setConnectionFactory(activeMQConnectionFactory());
        pooledConnectionFactory.setMaxConnections(maxConnections);
        pooledConnectionFactory.setMaximumActiveSessionPerConnection(maxActiveSessionsPerConnection);
        pooledConnectionFactory.setCreateConnectionOnStartup(true);
        pooledConnectionFactory.setIdleTimeout(30000);

        // Additional pooling settings to prevent connection issues
        pooledConnectionFactory.setBlockIfSessionPoolIsFull(true);
        pooledConnectionFactory.setBlockIfSessionPoolIsFullTimeout(5000);
        pooledConnectionFactory.setUseAnonymousProducers(false);
        pooledConnectionFactory.setTimeBetweenExpirationCheckMillis(5000);

        return pooledConnectionFactory;
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        // Use CachingConnectionFactory for additional connection management
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
        cachingConnectionFactory.setTargetConnectionFactory(pooledConnectionFactory());
        cachingConnectionFactory.setSessionCacheSize(maxActiveSessionsPerConnection);
        cachingConnectionFactory.setCacheConsumers(true);
        cachingConnectionFactory.setCacheProducers(true);
        return cachingConnectionFactory;
    }

    @Bean
    public JmsTemplate jmsTemplate() {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(connectionFactory());
        jmsTemplate.setDefaultDestinationName("demo.queue");
        jmsTemplate.setDeliveryPersistent(true);
        jmsTemplate.setSessionTransacted(false);
        return jmsTemplate;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setConcurrency(minConcurrency + "-" + maxConcurrency);
        factory.setSessionTransacted(false);
        factory.setAutoStartup(true);
        factory.setReceiveTimeout(10000L); // Increased timeout
        factory.setRecoveryInterval(5000L);

        // Additional settings to improve connection stability
        factory.setMaxMessagesPerTask(10);
//        factory.setIdleConsumerLimit(1);
//        factory.setIdleTaskExecutionLimit(30);

        return factory;
    }
}
