package com.rejection.service.controller;

import com.rejection.service.business.RejectionService;
import com.rejection.service.model.RejectionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * REST Controller for rejection API endpoints
 * Provides secure, logged, and monitored access to rejection services
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RejectionController {
    
    private static final Logger logger = LoggerFactory.getLogger(RejectionController.class);
    
    private final RejectionService rejectionService;
    
    @Autowired
    public RejectionController(RejectionService rejectionService) {
        this.rejectionService = rejectionService;
        logger.info("RejectionController initialized successfully");
    }
    
    /**
     * Get random rejection reason
     * Primary API endpoint with comprehensive logging and security headers
     */
    @GetMapping("/rejection")
    public ResponseEntity<RejectionResponse> getRandomRejection(HttpServletRequest request) {
        
        String clientIp = getClientIpAddress(request);
        String userAgent = sanitizeForLogging(request.getHeader("User-Agent"));
        
        logger.info("Processing rejection request from IP: {}, User-Agent: {}", clientIp, userAgent);
        
        try {
            // Generate rejection response
            RejectionResponse response = rejectionService.getRandomRejection();
            
            logger.info("Successfully generated rejection response {} for client {}", 
                       response.getRequestId(), clientIp);
            
            // Return response with security headers
            return ResponseEntity.ok()
                    .header("X-Request-ID", response.getRequestId())
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(response);
                    
        } catch (IllegalStateException e) {
            logger.error("Service unavailable for client {}: {}", clientIp, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error processing request from {}: {}", clientIp, e.getMessage(), e);
            throw new RuntimeException("Internal server error", e);
        }
    }
    
    /**
     * Health check endpoint with service statistics
     */
    @GetMapping("/health")
    @Cacheable(value = "health", key = "'status'")
    public ResponseEntity<Map<String, Object>> health() {
        logger.debug("Health check requested");
        
        try {
            RejectionService.ServiceStats stats = rejectionService.getServiceStatistics();
            
            Map<String, Object> healthData = Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "statistics", Map.of(
                    "totalRequests", stats.getTotalRequests(),
                    "successfulRequests", stats.getSuccessfulRequests(),
                    "errorRequests", stats.getErrorRequests(),
                    "cacheInitialized", stats.isCacheInitialized(),
                    "successRate", String.format("%.2f%%", stats.getSuccessRate()),
                    "cacheSize", stats.getCacheSize()
                )
            );
            
            return ResponseEntity.ok()
                    .header("Cache-Control", "no-cache")
                    .body(healthData);
                    
        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage(), e);
            
            Map<String, Object> errorHealth = Map.of(
                "status", "DOWN",
                "timestamp", LocalDateTime.now(),
                "error", e.getMessage()
            );
            
            return ResponseEntity.status(503).body(errorHealth);
        }
    }
    
    /**
     * Extract client IP address from request headers
     * Handles proxy headers for accurate IP identification with comprehensive error handling
     */
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            logger.warn("HttpServletRequest is null, returning default IP");
            return "0.0.0.0";
        }
        
        try {
            // Check X-Forwarded-For header (proxy/load balancer)
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.trim().isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
                String[] ips = xForwardedFor.split(",");
                if (ips.length > 0 && ips[0] != null) {
                    return ips[0].trim();
                }
            }
            
            // Check X-Real-IP header (nginx proxy)
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.trim().isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
                return xRealIp.trim();
            }
            
            // Fallback to remote address
            String remoteAddr = request.getRemoteAddr();
            return (remoteAddr != null && !remoteAddr.trim().isEmpty()) ? remoteAddr : "0.0.0.0";
            
        } catch (Exception e) {
            logger.error("Error extracting client IP address: {}", e.getMessage(), e);
            return "0.0.0.0";
        }
    }
    
    /**
     * Sanitize input for safe logging to prevent log injection attacks
     * Removes or replaces characters that could be used for log forging
     */
    private String sanitizeForLogging(String input) {
        if (input == null) {
            return "null";
        }
        // Remove line breaks, carriage returns, and other control characters
        String sanitized = input.replaceAll("[\\r\\n\\t\\x00-\\x1f\\x7f-\\x9f]", "_");
        return sanitized.substring(0, Math.min(sanitized.length(), 200)); // Limit length
    }
}