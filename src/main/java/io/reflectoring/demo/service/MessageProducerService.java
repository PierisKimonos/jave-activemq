package io.reflectoring.demo.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.reflectoring.demo.model.DemoMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MessageProducerService {

    private static final Logger logger = LoggerFactory.getLogger(MessageProducerService.class);

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    @Value("${demo.queue.name}")
    private String queueName;

    @Value("${demo.message.count}")
    private int messageCount;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final AtomicInteger messageCounter = new AtomicInteger(0);

    // Metrics
    private final Counter messagesPublishedCounter;
    private final Counter publishErrorsCounter;
    private final Timer publishTimer;

    public MessageProducerService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.messagesPublishedCounter = Counter.builder("messages.published.total")
                .description("Total number of messages published")
                .register(meterRegistry);
        this.publishErrorsCounter = Counter.builder("messages.publish.errors.total")
                .description("Total number of publish errors")
                .register(meterRegistry);
        this.publishTimer = Timer.builder("messages.publish.duration")
                .description("Time taken to publish a batch of messages")
                .register(meterRegistry);
    }    public CompletableFuture<String> publishMessages() {
        return CompletableFuture.supplyAsync(() -> {
            String batchId = UUID.randomUUID().toString();
            logger.info("Starting to publish {} messages with batch ID: {}", messageCount, batchId);
            
            Timer.Sample sample = Timer.start(meterRegistry);
            messageCounter.set(0);

            try {
                for (int i = 1; i <= messageCount; i++) {
                    DemoMessage message = new DemoMessage(
                        UUID.randomUUID().toString(),
                        "Message content for sequence " + i + " in batch " + batchId,
                        i
                    );

                    int finalI = i;
                    jmsTemplate.convertAndSend(queueName, message, messageObj -> {
                        messageObj.setStringProperty("batchId", batchId);
                        messageObj.setIntProperty("sequenceNumber", finalI);
                        return messageObj;
                    });

                    messagesPublishedCounter.increment();
                    messageCounter.incrementAndGet();

                    // Log progress every 100 messages
                    if (i % 100 == 0) {
                        logger.info("Published {} messages out of {}", i, messageCount);                    }
                }

                sample.stop(publishTimer);
                String result = String.format("Successfully published %d messages. Batch ID: %s", 
                    messageCount, batchId);
                logger.info(result);
                return result;

            } catch (Exception e) {
                publishErrorsCounter.increment();
                sample.stop(publishTimer);
                String errorMsg = "Error publishing messages: " + e.getMessage();
                logger.error(errorMsg, e);
                throw new RuntimeException(errorMsg, e);
            }
        }, executorService);
    }

    public int getPublishedMessageCount() {
        return messageCounter.get();
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
