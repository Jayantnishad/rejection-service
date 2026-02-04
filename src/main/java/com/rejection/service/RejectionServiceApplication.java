package com.rejection.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Main Spring Boot Application for Rejection as a Service
 * 
 * Business Logic: Entry point for rejection service with caching enabled
 * Security: Secure startup with comprehensive logging for monitoring
 * Performance: Optimized Spring Boot configuration for high throughput
 */
@SpringBootApplication
@EnableCaching
public class RejectionServiceApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(RejectionServiceApplication.class);
    
    /**
     * Application entry point with comprehensive startup logging
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        logger.info("Starting Rejection as a Service application...");
        logger.info("Application arguments: {}", java.util.Arrays.toString(args));
        
        try {
            var context = SpringApplication.run(RejectionServiceApplication.class, args);
            
            logger.info("Rejection as a Service started successfully!");
            logger.info("Application context loaded with {} beans", context.getBeanDefinitionCount());
            logger.info("API available at: http://localhost:8080/api/v1/rejection");
            logger.info("Health check at: http://localhost:8080/api/v1/health");
            logger.info("Actuator endpoints at: http://localhost:8080/actuator");
            
            // Add shutdown hook for proper cleanup logging
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down Rejection as a Service application...");
            }));
            
        } catch (Exception e) {
            logger.error("Failed to start Rejection as a Service: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}