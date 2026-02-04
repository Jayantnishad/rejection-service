# Technical Architecture - Rejection as a Service

## Thought Process & Decision Rationale

### Why Spring Boot?
**Reasoning**: Mature ecosystem, excellent performance, built-in security features, easy deployment
**Alternatives Considered**: Node.js (rejected due to single-threaded limitations), Go (rejected due to development speed)

### Why In-Memory Cache Only?
**Reasoning**: 
- 100 static rejection reasons = minimal memory footprint (~10KB)
- No data persistence needed for static content
- Eliminates database latency and complexity
- Reduces infrastructure costs to zero

### Why No Load Balancer?
**Reasoning**:
- Single instance can handle millions of requests with proper optimization
- Stateless design allows horizontal scaling when needed
- Free deployment platforms provide built-in load balancing

## Detailed Security Architecture

### 1. Multi-Layer Rate Limiting
```java
@Component
public class AdvancedRateLimiter {
    // Layer 1: Global rate limit (10,000 req/min)
    // Layer 2: Per-IP rate limit (100 req/min)
    // Layer 3: Burst protection (10 req/sec)
    // Layer 4: Suspicious pattern detection
}
```

### 2. Request Validation Pipeline
```java
@Component
public class SecurityFilter {
    // Validate HTTP method (GET only)
    // Check User-Agent header
    // Validate request size
    // Block known bot patterns
    // Implement CAPTCHA for suspicious IPs
}
```

### 3. Response Security
```java
@Configuration
public class SecurityHeaders {
    // X-Content-Type-Options: nosniff
    // X-Frame-Options: DENY
    // X-XSS-Protection: 1; mode=block
    // Content-Security-Policy: default-src 'self'
    // Strict-Transport-Security: max-age=31536000
}
```

## Million Request Handling Strategy

### 1. JVM Optimization
```bash
# Memory Management
-Xms1g -Xmx2g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100
-XX:+UseStringDeduplication

# Performance Tuning
-XX:+OptimizeStringConcat
-XX:+UseFastAccessorMethods
-Djava.awt.headless=true
```

### 2. Connection Pool Tuning
```yaml
server:
  tomcat:
    max-connections: 20000
    max-threads: 400
    min-spare-threads: 50
    accept-count: 200
    connection-timeout: 5000
```

### 3. Response Caching Strategy
```java
@RestController
public class RejectionController {
    
    @GetMapping(value = "/api/v1/rejection")
    @Cacheable(value = "rejections", key = "#root.method.name")
    public ResponseEntity<RejectionResponse> getRandomRejection() {
        // Cache responses for 1 hour
        // Reduce CPU usage by 90%
    }
}
```

### 4. Async Processing
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(200);
        executor.setQueueCapacity(1000);
        return executor;
    }
}
```

## Memory Optimization

### 1. Efficient Data Structures
```java
@Component
public class OptimizedRejectionCache {
    // Use String interning for memory efficiency
    // Pre-allocate ArrayList with exact capacity
    // Use primitive collections where possible
    
    private final String[] rejections = new String[100];
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
}
```

### 2. Object Pooling
```java
@Component
public class ResponsePool {
    // Pool RejectionResponse objects
    // Reduce garbage collection pressure
    // Improve response time consistency
}
```

## Monitoring & Observability

### 1. Custom Metrics
```java
@Component
public class MetricsCollector {
    private final MeterRegistry meterRegistry;
    
    // Request count per endpoint
    // Response time distribution
    // Memory usage tracking
    // Error rate monitoring
    // Cache hit ratio
}
```

### 2. Health Checks
```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // Check memory usage < 80%
        // Verify cache initialization
        // Test random number generation
        // Validate response time < 50ms
    }
}
```

## Deployment Architecture

### Railway Deployment (Recommended)
```dockerfile
FROM openjdk:17-jre-slim

COPY target/rejection-service.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", 
    "-Xms512m", 
    "-Xmx1g", 
    "-XX:+UseG1GC", 
    "-jar", 
    "/app.jar"]
```

### Environment Configuration
```yaml
# application-prod.yml
server:
  port: ${PORT:8080}
  compression:
    enabled: true
    mime-types: application/json,text/plain
    min-response-size: 1024

logging:
  level:
    com.rejection.service: INFO
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
```

## Performance Benchmarks

### Target Metrics
- **Throughput**: 10,000 requests/second
- **Latency**: P95 < 50ms, P99 < 100ms
- **Memory**: < 1GB under full load
- **CPU**: < 70% utilization at peak
- **Availability**: 99.95% uptime

### Load Testing Strategy
```bash
# Apache Bench
ab -n 1000000 -c 1000 http://localhost:8080/api/v1/rejection

# Expected Results:
# Requests per second: 8000-12000
# Time per request: 0.1-0.125ms (mean)
# Memory usage: 800MB-1GB
```

## Scaling Strategy

### Horizontal Scaling (When Needed)
1. **Stateless Design**: No session state, easy to replicate
2. **Load Balancer**: Add when single instance reaches limits
3. **CDN Integration**: Cache responses at edge locations
4. **Database Migration**: Only if dynamic content needed

### Vertical Scaling Limits
- **Memory**: 2GB maximum on free tiers
- **CPU**: 1-2 vCPUs typical
- **Network**: 100Mbps bandwidth limit

## Cost Analysis

### Free Tier Sustainability
- **Railway**: 500 hours/month = ~16 hours/day
- **Render**: 750 hours/month = ~25 hours/day
- **Google Cloud Run**: 2M requests/month free

### Upgrade Path
- **Railway Pro**: $5/month for unlimited hours
- **Render Standard**: $7/month for always-on
- **Cloud Run**: Pay-per-request beyond free tier

This architecture ensures maximum performance and security while maintaining zero infrastructure costs through intelligent design decisions and optimization strategies.