package io.reflectoring.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service to track parallel message processing statistics
 */
@Service
public class ParallelProcessingMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(ParallelProcessingMonitorService.class);

    // Track active processing threads
    private final AtomicInteger activeProcessingCount = new AtomicInteger(0);
    private final AtomicInteger peakConcurrentProcessing = new AtomicInteger(0);
    private final AtomicInteger totalProcessedMessages = new AtomicInteger(0);
    
    // Track processing threads by ID for detailed monitoring
    private final ConcurrentHashMap<String, ProcessingInfo> activeProcessingThreads = new ConcurrentHashMap<>();
    
    // Track processing times for averages
    private final AtomicInteger totalProcessingTime = new AtomicInteger(0);
    private final AtomicInteger processedCount = new AtomicInteger(0);

    /**
     * Call this when a message processing starts
     */
    public void onProcessingStart(String messageId) {
        int currentActive = activeProcessingCount.incrementAndGet();
        
        // Update peak if necessary
        peakConcurrentProcessing.updateAndGet(current -> Math.max(current, currentActive));
        
        // Track processing info
        ProcessingInfo info = new ProcessingInfo(messageId, Thread.currentThread().getName(), System.currentTimeMillis());
        activeProcessingThreads.put(messageId, info);
        
        logger.debug("Message processing started: {} on thread: {}, Active count: {}", 
            messageId, Thread.currentThread().getName(), currentActive);
    }    /**
     * Call this when a message processing completes
     */
    public void onProcessingComplete(String messageId) {
        // Prevent going negative - this can happen if the service was restarted while messages were processing
        int currentActive = activeProcessingCount.updateAndGet(current -> Math.max(0, current - 1));
        totalProcessedMessages.incrementAndGet();
        
        // Calculate processing time
        ProcessingInfo info = activeProcessingThreads.remove(messageId);
        if (info != null) {
            long processingTime = System.currentTimeMillis() - info.startTime;
            totalProcessingTime.addAndGet((int) processingTime);
            processedCount.incrementAndGet();
            
            logger.debug("Message processing completed: {} in {}ms, Active count: {}", 
                messageId, processingTime, currentActive);
        }
    }    /**
     * Call this when a message processing fails
     */
    public void onProcessingError(String messageId, Exception error) {
        // Prevent going negative - this can happen if the service was restarted while messages were processing
        int currentActive = activeProcessingCount.updateAndGet(current -> Math.max(0, current - 1));
        
        // Remove from active tracking
        ProcessingInfo info = activeProcessingThreads.remove(messageId);
        if (info != null) {
            long processingTime = System.currentTimeMillis() - info.startTime;
            logger.warn("Message processing failed: {} after {}ms on thread: {}, Active count: {}, Error: {}", 
                messageId, processingTime, info.threadName, currentActive, error.getMessage());
        }
    }

    /**
     * Get current parallel processing statistics
     */
    public ParallelProcessingStats getStats() {
        ParallelProcessingStats stats = new ParallelProcessingStats();
        stats.activeProcessingCount = activeProcessingCount.get();
        stats.peakConcurrentProcessing = peakConcurrentProcessing.get();
        stats.totalProcessedMessages = totalProcessedMessages.get();
        stats.activeThreadCount = activeProcessingThreads.size();
        
        // Calculate average processing time
        int processed = processedCount.get();
        if (processed > 0) {
            stats.averageProcessingTimeMs = totalProcessingTime.get() / processed;
        }
        
        // Get active thread details
        stats.activeThreadDetails = new String[activeProcessingThreads.size()];
        int i = 0;
        for (ProcessingInfo info : activeProcessingThreads.values()) {
            long duration = System.currentTimeMillis() - info.startTime;
            stats.activeThreadDetails[i++] = String.format("%s on %s (%dms)", 
                info.messageId, info.threadName, duration);
        }
        
        return stats;
    }    /**
     * Reset peak statistics (useful for monitoring specific periods)
     */
    public void resetPeakStats() {
        peakConcurrentProcessing.set(activeProcessingCount.get());
        logger.info("Peak concurrent processing statistics reset");
    }

    /**
     * Reset all parallel processing statistics and counters
     */
    public void resetAllStats() {
        activeProcessingCount.set(0);
        peakConcurrentProcessing.set(0);
        totalProcessedMessages.set(0);
        totalProcessingTime.set(0);
        processedCount.set(0);
        activeProcessingThreads.clear();
        logger.info("All parallel processing statistics reset");
    }

    /**
     * Get a summary for logging
     */
    public String getStatsString() {
        ParallelProcessingStats stats = getStats();
        return String.format("Active: %d, Peak: %d, Total: %d, Avg Time: %dms", 
            stats.activeProcessingCount, stats.peakConcurrentProcessing, 
            stats.totalProcessedMessages, stats.averageProcessingTimeMs);
    }

    private static class ProcessingInfo {
        final String messageId;
        final String threadName;
        final long startTime;

        ProcessingInfo(String messageId, String threadName, long startTime) {
            this.messageId = messageId;
            this.threadName = threadName;
            this.startTime = startTime;
        }
    }

    public static class ParallelProcessingStats {
        public int activeProcessingCount = 0;
        public int peakConcurrentProcessing = 0;
        public int totalProcessedMessages = 0;
        public int activeThreadCount = 0;
        public int averageProcessingTimeMs = 0;
        public String[] activeThreadDetails = new String[0];
    }
}
