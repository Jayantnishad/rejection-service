package com.rejection.service.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RejectionResponse model
 * Validates business logic and data integrity
 */
class RejectionResponseTest {
    
    @Test
    void testRejectionResponseCreation() {
        // Given: Valid input parameters
        int testId = 1;
        String testReason = "I'm focusing on my career right now";
        
        // When: Creating rejection response
        RejectionResponse response = new RejectionResponse(testId, testReason);
        
        // Then: All fields should be properly set
        assertEquals(testId, response.getId());
        assertEquals(testReason, response.getReason());
        assertNotNull(response.getTimestamp());
        assertNotNull(response.getRequestId());
        assertEquals(8, response.getRequestId().length());
    }
    
    @Test
    void testUniqueRequestIds() {
        // Given: Multiple responses
        RejectionResponse response1 = new RejectionResponse(1, "Test reason 1");
        RejectionResponse response2 = new RejectionResponse(2, "Test reason 2");
        
        // Then: Request IDs should be unique
        assertNotEquals(response1.getRequestId(), response2.getRequestId());
    }
}