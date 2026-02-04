package com.rejection.service.business;

import com.rejection.service.cache.RejectionCache;
import com.rejection.service.model.RejectionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Business service for rejection logic
 * Handles request processing, metrics collection, and business validation
 * 
 * Business Logic: Orchestrates rejection reason generation with proper tracking
 * Security: Input validation and request monitoring for abuse detection
 * Performance: Atomic counters for thread-safe metrics collection
 */
@Service
public class RejectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(RejectionService.class);
    
    private final RejectionCache rejectionCache;
    private final AtomicLong requestCounter = new AtomicLong(0);
    private final AtomicLong successCounter = new AtomicLong(0);
    private final AtomicLong errorCounter = new AtomicLong(0);
    
    /**
     * Constructor with dependency injection
     * 
     * @param rejectionCache Injected cache component for rejection reasons
     */
    @Autowired
    public RejectionService(RejectionCache rejectionCache) {
        this.rejectionCache = rejectionCache;
        logger.debug("RejectionService initialized with cache component");
    }
    
    /**
     * Generate random rejection response with comprehensive logging and metrics
     * 
     * Business Logic: Creates rejection response with unique tracking ID
     * Security: Validates cache state and logs all access attempts
     * Performance: Atomic counters for thread-safe metrics
     * 
     * @return RejectionResponse with random reason and metadata
     * @throws IllegalStateException if service is not ready
     */
    public RejectionResponse getRandomRejection() {
        long requestId = requestCounter.incrementAndGet();
        
        logger.info("Processing rejection request #{}", requestId);
        
        try {
            // Validate service readiness
            if (!rejectionCache.isInitialized()) {
                errorCounter.incrementAndGet();
                logger.error("Service not ready - cache not initialized for request #{}", requestId);
                throw new IllegalStateException("Rejection service not ready - cache not initialized");
            }
            
            // Get random reason from cache
            String reason = rejectionCache.getRandomReason();
            
            // Validate reason quality
            if (reason == null || reason.trim().isEmpty()) {
                errorCounter.incrementAndGet();
                logger.error("Invalid rejection reason retrieved for request #{}", requestId);
                throw new IllegalStateException("Invalid rejection reason retrieved from cache");
            }
            
            // Create response with business metadata
            RejectionResponse response = new RejectionResponse(requestId, reason);
            
            // Update success metrics
            successCounter.incrementAndGet();
            
            logger.info("Successfully generated rejection response for request #{}: requestId={}, reasonLength={}", 
                       requestId, response.getRequestId(), reason.length());
            
            // Log metrics every 100 requests for monitoring
            if (requestId % 100 == 0) {
                try {
                    logServiceMetrics();
                } catch (Exception metricsException) {
                    logger.warn("Failed to log service metrics: {}", metricsException.getMessage(), metricsException);
                }
            }
            
            return response;
            
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            logger.error("Failed to process rejection request #{}: {}", requestId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Get total number of requests processed
     * 
     * @return Total request count for monitoring and analytics
     */
    public long getTotalRequests() {
        return requestCounter.get();
    }
    
    /**
     * Get successful request count
     * 
     * @return Success count for service health monitoring
     */
    public long getSuccessfulRequests() {
        return successCounter.get();
    }
    
    /**
     * Get error request count
     * 
     * @return Error count for service health monitoring
     */
    public long getErrorRequests() {
        return errorCounter.get();
    }
    
    /**
     * Calculate success rate percentage
     * 
     * @return Success rate as percentage for monitoring dashboards
     */
    public double getSuccessRate() {
        long total = requestCounter.get();
        if (total == 0) {
            return 100.0; // No requests yet, assume healthy
        }
        return (double) successCounter.get() / total * 100.0;
    }
    
    /**
     * Check if service is healthy and ready to serve requests
     * 
     * Business Logic: Validates all dependencies and service state
     * 
     * @return true if service is ready, false otherwise
     */
    public boolean isServiceHealthy() {
        try {
            // Check cache initialization
            if (!rejectionCache.isInitialized()) {
                logger.warn("Service health check failed - cache not initialized");
                return false;
            }
            
            // Check cache size
            if (rejectionCache.getCacheSize() <= 0) {
                logger.warn("Service health check failed - invalid cache size: {}", 
                           rejectionCache.getCacheSize());
                return false;
            }
            
            // Check error rate (fail if > 50% errors in last requests)
            double successRate = getSuccessRate();
            if (successRate < 50.0 && requestCounter.get() > 10) {
                logger.warn("Service health check failed - low success rate: {}%", successRate);
                return false;
            }
            
            logger.debug("Service health check passed - success rate: {}%", successRate);
            return true;
            
        } catch (Exception e) {
            logger.error("Service health check failed with exception: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get comprehensive service statistics
     * 
     * @return ServiceStats object with all metrics
     */
    public ServiceStats getServiceStatistics() {
        try {
            return new ServiceStats(
                requestCounter.get(),
                successCounter.get(),
                errorCounter.get(),
                getSuccessRate(),
                rejectionCache.getCacheSize(),
                rejectionCache.isInitialized()
            );
        } catch (Exception e) {
            logger.error("Failed to get service statistics: {}", e.getMessage(), e);
            // Return default stats on error
            return new ServiceStats(0, 0, 0, 0.0, 0, false);
        }
    }
    
    /**
     * Log service metrics for monitoring and debugging
     * 
     * Performance Monitoring: Tracks service health and usage patterns
     */
    private void logServiceMetrics() {
        long total = requestCounter.get();
        long success = successCounter.get();
        long errors = errorCounter.get();
        double successRate = total == 0 ? 100.0 : (double) success / total * 100.0;
        
        logger.info("Service Metrics - Total: {}, Success: {}, Errors: {}, Success Rate: {}%, Cache Size: {}", 
                   total, success, errors, String.format("%.2f", successRate), 
                   rejectionCache.getCacheSize());
    }
    
    /**
     * Inner class for service statistics
     * Encapsulates all service metrics for monitoring
     */
    public static class ServiceStats {
        private final long totalRequests;
        private final long successfulRequests;
        private final long errorRequests;
        private final double successRate;
        private final int cacheSize;
        private final boolean cacheInitialized;
        
        public ServiceStats(long totalRequests, long successfulRequests, long errorRequests, 
                           double successRate, int cacheSize, boolean cacheInitialized) {
            this.totalRequests = totalRequests;
            this.successfulRequests = successfulRequests;
            this.errorRequests = errorRequests;
            this.successRate = successRate;
            this.cacheSize = cacheSize;
            this.cacheInitialized = cacheInitialized;
        }
        
        // Getters for all metrics
        public long getTotalRequests() { 
            return totalRequests; 
        }
        
        public long getSuccessfulRequests() { 
            return successfulRequests; 
        }
        
        public long getErrorRequests() { 
            return errorRequests; 
        }
        
        public double getSuccessRate() { 
            return successRate; 
        }
        
        public int getCacheSize() { 
            return cacheSize; 
        }
        
        public boolean isCacheInitialized() { 
            return cacheInitialized; 
        }
    }
}