package com.rejection.service.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response model for rejection API
 * Contains rejection reason with metadata for tracking and auditing
 * 
 * Business Logic: Each response includes unique ID, timestamp, and request tracking
 * Security: No sensitive data exposed, only rejection reasons and metadata
 */
public class RejectionResponse {
    
    private final long id;
    private final String reason;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private final LocalDateTime timestamp;
    
    private final String requestId;
    
    /**
     * Constructor for rejection response
     * 
     * @param id Sequential request identifier for tracking
     * @param reason The rejection message to be returned
     * 
     * Logging: Constructor automatically generates timestamp and unique request ID
     * Performance: Uses UUID substring for efficient request tracking
     * @throws IllegalArgumentException if parameters are invalid
     */
    public RejectionResponse(long id, String reason) {
        if (id < 0) {
            throw new IllegalArgumentException("ID must be non-negative, received: " + id);
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be null or empty");
        }
        
        this.id = id;
        this.reason = reason.trim();
        
        try {
            this.timestamp = LocalDateTime.now();
            this.requestId = UUID.randomUUID().toString().substring(0, 8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize RejectionResponse: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get sequential request ID
     * Used for request counting and basic analytics
     */
    public long getId() { 
        return id; 
    }
    
    /**
     * Get rejection reason message
     * Core business value - the actual rejection text
     */
    public String getReason() { 
        return reason; 
    }
    
    /**
     * Get response timestamp
     * ISO format for consistent API responses
     */
    public LocalDateTime getTimestamp() { 
        return timestamp; 
    }
    
    /**
     * Get unique request identifier
     * 8-character UUID for request tracking and debugging
     */
    public String getRequestId() { 
        return requestId; 
    }
}