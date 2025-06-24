package io.reflectoring.demo.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class DemoMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String content;
    private LocalDateTime timestamp;
    private int sequenceNumber;

    public DemoMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public DemoMessage(String id, String content, int sequenceNumber) {
        this();
        this.id = id;
        this.content = content;
        this.sequenceNumber = sequenceNumber;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public String toString() {
        return "DemoMessage{" +
                "id='" + id + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                ", sequenceNumber=" + sequenceNumber +
                '}';
    }
}
