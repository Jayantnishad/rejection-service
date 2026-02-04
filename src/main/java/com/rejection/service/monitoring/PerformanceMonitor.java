package com.rejection.service.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Performance monitoring component
 * Tracks memory usage and system metrics
 */
@Component
public class PerformanceMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitor.class);
    
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    @PostConstruct
    public void startMonitoring() {
        logger.info("Starting performance monitoring...");
        
        try {
            // Log memory usage every 5 minutes
            scheduler.scheduleAtFixedRate(this::logMemoryUsage, 0, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.error("Failed to start performance monitoring: {}", e.getMessage(), e);
            throw new IllegalStateException("Performance monitoring initialization failed", e);
        }
    }
    
    private void logMemoryUsage() {
        try {
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            
            double heapUsagePercent = (double) heapUsed / heapMax * 100;
            
            logger.info("Memory Usage - Heap: {}MB/{}MB ({}%), Non-Heap: {}MB",
                       heapUsed / 1024 / 1024,
                       heapMax / 1024 / 1024,
                       String.format("%.2f", heapUsagePercent),
                       nonHeapUsed / 1024 / 1024);
        } catch (ArithmeticException e) {
            logger.error("Memory calculation error (division by zero or overflow): {}", e.getMessage());
        } catch (SecurityException e) {
            logger.error("Security restriction accessing memory MXBean: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to log memory usage: {}", e.getMessage(), e);
        }
    }
    
    @PreDestroy
    public void stopMonitoring() {
        try {
            logger.info("Stopping performance monitoring...");
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (Exception e) {
            logger.error("Error stopping performance monitoring: {}", e.getMessage(), e);
        }
    }
}