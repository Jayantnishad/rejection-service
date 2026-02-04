package com.rejection.service.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting filter using token bucket algorithm
 * Security: Prevents abuse and ensures fair usage per IP
 * Performance: Thread-safe concurrent implementation
 */
@Component
public class RateLimitingFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    
    private static final int REQUESTS_PER_MINUTE = 100;
    
    private final ConcurrentHashMap<String, BucketEntry> buckets = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
    
    public RateLimitingFilter() {
        try {
            // Clean up old buckets every 10 minutes
            cleanupScheduler.scheduleAtFixedRate(this::cleanupOldBuckets, 10, 10, TimeUnit.MINUTES);
            logger.info("Rate limiting filter initialized with cleanup scheduler");
        } catch (Exception e) {
            logger.error("Failed to initialize rate limiting cleanup scheduler: {}", e.getMessage(), e);
            throw new IllegalStateException("Rate limiting filter initialization failed", e);
        }
    }
    
    private static class BucketEntry {
        final Bucket bucket;
        volatile LocalDateTime lastAccess;
        
        BucketEntry(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccess = LocalDateTime.now();
        }
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Input validation
        if (!isValidRequest(httpRequest)) {
            logger.warn("Invalid request blocked from IP: {}", getClientIpAddress(httpRequest));
            httpResponse.setStatus(400);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                "{\"error\":\"Bad Request\",\"message\":\"Invalid request format\"}"
            );
            return;
        }
        
        String clientIp = getClientIpAddress(httpRequest);
        
        // Check for suspicious patterns
        if (isSuspiciousRequest(httpRequest)) {
            logger.warn("Suspicious request blocked from IP: {}", clientIp);
            httpResponse.setStatus(403);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                "{\"error\":\"Forbidden\",\"message\":\"Suspicious activity detected\"}"
            );
            return;
        }
        
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::createNewBucketEntry).bucket;
        buckets.get(clientIp).lastAccess = LocalDateTime.now();
        
        if (bucket.tryConsume(1)) {
            logger.debug("Request allowed for IP: {}", clientIp);
            chain.doFilter(request, response);
        } else {
            logger.warn("Rate limit exceeded for IP: {}", clientIp);
            
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Try again later.\"}"
            );
        }
    }
    
    private BucketEntry createNewBucketEntry(String ip) {
        logger.info("Creating new rate limit bucket for IP: {}", ip);
        
        Bandwidth limit = Bandwidth.classic(REQUESTS_PER_MINUTE, 
                                          Refill.intervally(REQUESTS_PER_MINUTE, Duration.ofMinutes(1)));
        
        Bucket bucket = Bucket4j.builder()
                .addLimit(limit)
                .build();
                
        return new BucketEntry(bucket);
    }
    
    private void cleanupOldBuckets() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
            int removedCount = 0;
            var iterator = buckets.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (entry.getValue().lastAccess.isBefore(cutoff)) {
                    iterator.remove();
                    removedCount++;
                }
            }
            if (removedCount > 0) {
                logger.debug("Cleaned up {} old rate limit buckets", removedCount);
            }
        } catch (Exception e) {
            logger.error("Error during bucket cleanup: {}", e.getMessage(), e);
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        try {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                String[] ips = xForwardedFor.split(",");
                if (ips.length > 0 && ips[0] != null) {
                    return ips[0].trim();
                }
            }
            
            String remoteAddr = request.getRemoteAddr();
            return remoteAddr != null ? remoteAddr : "0.0.0.0";
        } catch (Exception e) {
            logger.warn("Error extracting client IP address: {}", e.getMessage());
            return "0.0.0.0";
        }
    }
    
    private boolean isValidRequest(HttpServletRequest request) {
        // Validate HTTP method
        if (!"GET".equals(request.getMethod())) {
            return false;
        }
        
        // Validate request size (headers + body)
        String contentLength = request.getHeader("Content-Length");
        if (contentLength != null) {
            try {
                int length = Integer.parseInt(contentLength);
                if (length > 1024) { // Max 1KB
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean isSuspiciousRequest(HttpServletRequest request) {
        try {
            String userAgent = request.getHeader("User-Agent");
            
            // Block requests without User-Agent
            if (userAgent == null || userAgent.trim().isEmpty()) {
                return true;
            }
            
            // Block known bot patterns
            String userAgentLower = userAgent.toLowerCase();
            String[] suspiciousPatterns = {
                "bot", "crawler", "spider", "scraper", "curl", "wget", "python", "java"
            };
            
            for (String pattern : suspiciousPatterns) {
                if (userAgentLower.contains(pattern)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            logger.warn("Error checking suspicious request: {}", e.getMessage());
            return true; // Fail secure - treat as suspicious if we can't validate
        }
    }
}