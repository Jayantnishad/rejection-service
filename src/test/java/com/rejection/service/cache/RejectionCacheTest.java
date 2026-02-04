package com.rejection.service.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Unit tests for RejectionCache component
 * Validates thread safety, performance, and business logic
 */
class RejectionCacheTest {
    
    private RejectionCache rejectionCache;
    
    @BeforeEach
    void setUp() {
        rejectionCache = new RejectionCache();
    }
    
    @Test
    void testCacheInitialization() {
        // Given: Fresh cache instance
        assertFalse(rejectionCache.isInitialized());
        
        // When: Initialize cache
        rejectionCache.initializeReasons();
        
        // Then: Cache should be properly initialized
        assertTrue(rejectionCache.isInitialized());
        assertEquals(100, rejectionCache.getCacheSize());
    }
    
    @Test
    void testGetRandomReasonBeforeInitialization() {
        // Given: Uninitialized cache
        assertFalse(rejectionCache.isInitialized());
        
        // When/Then: Should throw exception
        assertThrows(IllegalStateException.class, () -> {
            rejectionCache.getRandomReason();
        });
    }
    
    @Test
    void testGetRandomReasonAfterInitialization() {
        // Given: Initialized cache
        rejectionCache.initializeReasons();
        
        // When: Get random reason
        String reason = rejectionCache.getRandomReason();
        
        // Then: Should return valid reason
        assertNotNull(reason);
        assertFalse(reason.trim().isEmpty());
        assertTrue(reason.length() > 5); // Reasonable minimum length
    }
    
    @Test
    void testRandomnessDistribution() {
        // Given: Initialized cache
        rejectionCache.initializeReasons();
        
        // When: Get multiple random reasons
        Set<String> uniqueReasons = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            uniqueReasons.add(rejectionCache.getRandomReason());
        }
        
        // Then: Should have reasonable distribution (at least 10 unique reasons)
        assertTrue(uniqueReasons.size() >= 10, 
                  "Expected at least 10 unique reasons, got: " + uniqueReasons.size());
    }
    
    @Test
    void testThreadSafety() throws InterruptedException {
        // Given: Initialized cache
        rejectionCache.initializeReasons();
        
        // When: Access cache from multiple threads
        Thread[] threads = new Thread[10];
        Set<String> allReasons = new HashSet<>();
        
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    String reason = rejectionCache.getRandomReason();
                    synchronized (allReasons) {
                        allReasons.add(reason);
                    }
                }
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then: Should have collected multiple unique reasons without errors
        assertTrue(allReasons.size() >= 5, 
                  "Expected multiple unique reasons from concurrent access");
    }
    
    @Test
    void testCacheConsistency() {
        // Given: Initialized cache
        rejectionCache.initializeReasons();
        
        // When: Get same cache size multiple times
        int size1 = rejectionCache.getCacheSize();
        int size2 = rejectionCache.getCacheSize();
        
        // Then: Size should be consistent
        assertEquals(size1, size2);
        assertEquals(100, size1);
    }
}