package com.rejection.service.business;

import com.rejection.service.cache.RejectionCache;
import com.rejection.service.model.RejectionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RejectionService business logic
 * Validates service behavior, metrics, and error handling
 */
class RejectionServiceTest {
    
    @Mock
    private RejectionCache mockCache;
    
    private RejectionService rejectionService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rejectionService = new RejectionService(mockCache);
    }
    
    @Test
    void testGetRandomRejectionSuccess() {
        // Given: Initialized cache with valid reason
        when(mockCache.isInitialized()).thenReturn(true);
        when(mockCache.getRandomReason()).thenReturn("I'm focusing on my career right now");
        
        // When: Get random rejection
        RejectionResponse response = rejectionService.getRandomRejection();
        
        // Then: Should return valid response
        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("I'm focusing on my career right now", response.getReason());
        assertNotNull(response.getTimestamp());
        assertNotNull(response.getRequestId());
        
        // Verify metrics
        assertEquals(1, rejectionService.getTotalRequests());
        assertEquals(1, rejectionService.getSuccessfulRequests());
        assertEquals(0, rejectionService.getErrorRequests());
    }
    
    @Test
    void testGetRandomRejectionCacheNotInitialized() {
        // Given: Uninitialized cache
        when(mockCache.isInitialized()).thenReturn(false);
        
        // When/Then: Should throw exception
        assertThrows(IllegalStateException.class, () -> {
            rejectionService.getRandomRejection();
        });
        
        // Verify metrics
        assertEquals(1, rejectionService.getTotalRequests());
        assertEquals(0, rejectionService.getSuccessfulRequests());
        assertEquals(1, rejectionService.getErrorRequests());
    }
    
    @Test
    void testGetRandomRejectionInvalidReason() {
        // Given: Cache returns invalid reason
        when(mockCache.isInitialized()).thenReturn(true);
        when(mockCache.getRandomReason()).thenReturn("");
        
        // When/Then: Should throw exception
        assertThrows(IllegalStateException.class, () -> {
            rejectionService.getRandomRejection();
        });
        
        // Verify error metrics
        assertEquals(1, rejectionService.getErrorRequests());
    }
    
    @Test
    void testGetRandomRejectionNullReason() {
        // Given: Cache returns null reason
        when(mockCache.isInitialized()).thenReturn(true);
        when(mockCache.getRandomReason()).thenReturn(null);
        
        // When/Then: Should throw exception
        assertThrows(IllegalStateException.class, () -> {
            rejectionService.getRandomRejection();
        });
        
        // Verify error metrics
        assertEquals(1, rejectionService.getErrorRequests());
    }
    
    @Test
    void testMultipleRequests() {
        // Given: Initialized cache
        when(mockCache.isInitialized()).thenReturn(true);
        when(mockCache.getRandomReason()).thenReturn("Test reason");
        
        // When: Make multiple requests
        for (int i = 0; i < 5; i++) {
            RejectionResponse response = rejectionService.getRandomRejection();
            assertNotNull(response);
            assertEquals(i + 1, response.getId());
        }
        
        // Then: Verify metrics
        assertEquals(5, rejectionService.getTotalRequests());
        assertEquals(5, rejectionService.getSuccessfulRequests());
        assertEquals(0, rejectionService.getErrorRequests());
        assertEquals(100.0, rejectionService.getSuccessRate());
    }
    
    @Test
    void testSuccessRateCalculation() {
        // Given: Mix of successful and failed requests
        when(mockCache.isInitialized()).thenReturn(true, false, true);
        when(mockCache.getRandomReason()).thenReturn("Test reason");
        
        // When: Make requests with mixed results
        rejectionService.getRandomRejection(); // Success
        
        try {
            rejectionService.getRandomRejection(); // Failure (cache not initialized)
        } catch (IllegalStateException e) {
            // Expected
        }
        
        rejectionService.getRandomRejection(); // Success
        
        // Then: Verify success rate
        assertEquals(3, rejectionService.getTotalRequests());
        assertEquals(2, rejectionService.getSuccessfulRequests());
        assertEquals(1, rejectionService.getErrorRequests());
        assertEquals(66.67, rejectionService.getSuccessRate(), 0.01);
    }
    
    @Test
    void testServiceHealthyWhenCacheInitialized() {
        // Given: Properly initialized cache
        when(mockCache.isInitialized()).thenReturn(true);
        when(mockCache.getCacheSize()).thenReturn(100);
        
        // When: Check service health
        boolean healthy = rejectionService.isServiceHealthy();
        
        // Then: Should be healthy
        assertTrue(healthy);
    }
    
    @Test
    void testServiceUnhealthyWhenCacheNotInitialized() {
        // Given: Uninitialized cache
        when(mockCache.isInitialized()).thenReturn(false);
        
        // When: Check service health
        boolean healthy = rejectionService.isServiceHealthy();
        
        // Then: Should be unhealthy
        assertFalse(healthy);
    }
    
    @Test
    void testServiceUnhealthyWithInvalidCacheSize() {
        // Given: Cache with invalid size
        when(mockCache.isInitialized()).thenReturn(true);
        when(mockCache.getCacheSize()).thenReturn(0);
        
        // When: Check service health
        boolean healthy = rejectionService.isServiceHealthy();
        
        // Then: Should be unhealthy
        assertFalse(healthy);
    }
    
    @Test
    void testGetServiceStatistics() {
        // Given: Service with some activity
        when(mockCache.isInitialized()).thenReturn(true);
        when(mockCache.getCacheSize()).thenReturn(100);
        when(mockCache.getRandomReason()).thenReturn("Test reason");
        
        // Make some requests
        rejectionService.getRandomRejection();
        rejectionService.getRandomRejection();
        
        // When: Get statistics
        RejectionService.ServiceStats stats = rejectionService.getServiceStatistics();
        
        // Then: Verify all statistics
        assertEquals(2, stats.getTotalRequests());
        assertEquals(2, stats.getSuccessfulRequests());
        assertEquals(0, stats.getErrorRequests());
        assertEquals(100.0, stats.getSuccessRate());
        assertEquals(100, stats.getCacheSize());
        assertTrue(stats.isCacheInitialized());
    }
    
    @Test
    void testConcurrentRequests() throws InterruptedException {
        // Given: Initialized cache
        when(mockCache.isInitialized()).thenReturn(true);
        when(mockCache.getRandomReason()).thenReturn("Concurrent test reason");
        
        // When: Make concurrent requests
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    rejectionService.getRandomRejection();
                }
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then: Verify all requests were processed
        assertEquals(100, rejectionService.getTotalRequests());
        assertEquals(100, rejectionService.getSuccessfulRequests());
        assertEquals(0, rejectionService.getErrorRequests());
        assertEquals(100.0, rejectionService.getSuccessRate());
    }
}