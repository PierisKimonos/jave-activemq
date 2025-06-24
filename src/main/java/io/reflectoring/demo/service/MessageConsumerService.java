package io.reflectoring.demo.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.reflectoring.demo.model.DemoMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MessageConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(MessageConsumerService.class);

    @Value("${spring.jms.listener.min-concurrency:1}")
    private int minConcurrency;

    @Value("${spring.jms.listener.max-concurrency:10}")
    private int maxConcurrency;

    private final AtomicInteger consumedMessageCount = new AtomicInteger(0);
    private final AtomicInteger processedMessageCount = new AtomicInteger(0);

    // Metrics
    private final Counter messagesConsumedCounter;
    private final Counter processingErrorsCounter;
    private final Timer messageProcessingTimer;
    private final MeterRegistry meterRegistry;
    
    // Parallel processing monitor
    private final ParallelProcessingMonitorService parallelProcessingMonitor;

    public MessageConsumerService(MeterRegistry meterRegistry, ParallelProcessingMonitorService parallelProcessingMonitor) {
        this.meterRegistry = meterRegistry;
        this.parallelProcessingMonitor = parallelProcessingMonitor;
        this.messagesConsumedCounter = Counter.builder("messages.consumed.total")
                .description("Total number of messages consumed")
                .register(meterRegistry);
        this.processingErrorsCounter = Counter.builder("messages.processing.errors.total")
                .description("Total number of message processing errors")
                .register(meterRegistry);
        this.messageProcessingTimer = Timer.builder("messages.processing.duration")
                .description("Time taken to process a message")
                .register(meterRegistry);
    }    @JmsListener(
        destination = "${demo.queue.name}", 
        containerFactory = "jmsListenerContainerFactory",
        concurrency = "${spring.jms.listener.min-concurrency:1}-${spring.jms.listener.max-concurrency:10}"
    )
    public void receiveMessage(
            @Payload DemoMessage message,
            @Header(value = "batchId", required = false) String batchId,
            @Header(value = "sequenceNumber", required = false) Integer sequenceNumber) {
        
        Timer.Sample sample = Timer.start(meterRegistry);
        String messageId = message.getId();
        
        // Start parallel processing monitoring
        parallelProcessingMonitor.onProcessingStart(messageId);
        
        try {
            consumedMessageCount.incrementAndGet();
            messagesConsumedCounter.increment();
            
            // Simulate processing time
            Thread.sleep(10); // 10ms processing time per message
            
            logger.debug("Consumed message - ID: {}, Sequence: {}, Batch: {}, Thread: {}", 
                messageId, 
                sequenceNumber != null ? sequenceNumber : message.getSequenceNumber(),
                batchId, 
                Thread.currentThread().getName());

            processedMessageCount.incrementAndGet();
            sample.stop(messageProcessingTimer);
            
            // Complete parallel processing monitoring
            parallelProcessingMonitor.onProcessingComplete(messageId);

            // Log progress every 100 messages with parallel processing stats
            int currentCount = processedMessageCount.get();
            if (currentCount % 100 == 0) {
                String parallelStats = parallelProcessingMonitor.getStatsString();
                logger.info("Processed {} messages so far on thread: {} | Parallel Processing: {}", 
                    currentCount, Thread.currentThread().getName(), parallelStats);
            }
        } catch (InterruptedException e) {
            sample.stop(messageProcessingTimer);
            processingErrorsCounter.increment();
            parallelProcessingMonitor.onProcessingError(messageId, e);
            Thread.currentThread().interrupt();
            logger.error("Message processing interrupted for message ID: {}", messageId, e);        } catch (Exception e) {
            sample.stop(messageProcessingTimer);
            processingErrorsCounter.increment();
            parallelProcessingMonitor.onProcessingError(messageId, e);
            logger.error("Error processing message ID: {} - Error: {}", messageId, e.getMessage(), e);
            // In a real scenario, you might want to send to a dead letter queue
        }
    }

    public int getConsumedMessageCount() {
        return consumedMessageCount.get();
    }

    public int getProcessedMessageCount() {
        return processedMessageCount.get();
    }

    public void resetCounters() {
        consumedMessageCount.set(0);
        processedMessageCount.set(0);
        logger.info("Message counters reset");
    }

    public int getMinConcurrency() {
        return minConcurrency;
    }

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public String getConcurrencyRange() {
        return minConcurrency + "-" + maxConcurrency;
    }
}
