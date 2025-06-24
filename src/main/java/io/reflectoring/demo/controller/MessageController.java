package io.reflectoring.demo.controller;

import io.reflectoring.demo.service.MessageConsumerService;
import io.reflectoring.demo.service.MessageProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private MessageProducerService messageProducerService;

    @Autowired
    private MessageConsumerService messageConsumerService;

    @GetMapping("/publish")
    public ResponseEntity<Map<String, Object>> publishMessages() {
        logger.info("Received request to publish 1000 messages");
        
        try {
            CompletableFuture<String> future = messageProducerService.publishMessages();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "STARTED");
            response.put("message", "Publishing 1000 messages started");
            response.put("timestamp", System.currentTimeMillis());
            
            // Asynchronously log the result
            future.thenAccept(result -> {
                logger.info("Publishing completed: {}", result);
            }).exceptionally(throwable -> {
                logger.error("Publishing failed: {}", throwable.getMessage());
                return null;
            });
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error starting message publishing", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Failed to start publishing: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("publishedMessages", messageProducerService.getPublishedMessageCount());
        status.put("consumedMessages", messageConsumerService.getConsumedMessageCount());
        status.put("processedMessages", messageConsumerService.getProcessedMessageCount());
        status.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(status);
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetCounters() {
        messageConsumerService.resetCounters();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Counters reset successfully");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Message Service");
        return ResponseEntity.ok(health);
    }
}
