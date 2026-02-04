# UML Diagrams - Rejection as a Service

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Internet Users                            │
└─────────────────────┬───────────────────────────────────────────┘
                      │ HTTPS Requests
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                    CDN / Edge Cache                              │
│                 (Cloudflare Free Tier)                          │
└─────────────────────┬───────────────────────────────────────────┘
                      │ Cache Miss
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                 Railway/Render Platform                          │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │              Spring Boot Application                        ││
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ ││
│  │  │ Security Filter │  │ Rate Limiter    │  │ CORS Config │ ││
│  │  └─────────────────┘  └─────────────────┘  └─────────────┘ ││
│  │  ┌─────────────────────────────────────────────────────────┐ ││
│  │  │            REST Controller Layer                        │ ││
│  │  │  GET /api/v1/rejection                                  │ ││
│  │  │  GET /health                                            │ ││
│  │  └─────────────────────────────────────────────────────────┘ ││
│  │  ┌─────────────────────────────────────────────────────────┐ ││
│  │  │            Service Layer                                │ ││
│  │  │  RejectionService                                       │ ││
│  │  └─────────────────────────────────────────────────────────┘ ││
│  │  ┌─────────────────────────────────────────────────────────┐ ││
│  │  │            In-Memory Cache                              │ ││
│  │  │  ArrayList<String> rejectionReasons                    │ ││
│  │  │  ThreadLocalRandom                                      │ ││
│  │  └─────────────────────────────────────────────────────────┘ ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

## Class Diagram

```
┌─────────────────────────────────────────┐
│            RejectionController          │
├─────────────────────────────────────────┤
│ - rejectionService: RejectionService    │
│ - meterRegistry: MeterRegistry          │
├─────────────────────────────────────────┤
│ + getRandomRejection(): ResponseEntity  │
│ + getHealth(): ResponseEntity           │
│ + getMetrics(): ResponseEntity          │
└─────────────────┬───────────────────────┘
                  │ depends on
                  ▼
┌─────────────────────────────────────────┐
│            RejectionService             │
├─────────────────────────────────────────┤
│ - rejectionCache: RejectionCache        │
│ - random: ThreadLocalRandom             │
│ - requestCounter: AtomicLong            │
├─────────────────────────────────────────┤
│ + getRandomRejection(): RejectionResponse│
│ + getStatistics(): ServiceStats         │
│ - validateRequest(): boolean            │
└─────────────────┬───────────────────────┘
                  │ uses
                  ▼
┌─────────────────────────────────────────┐
│            RejectionCache               │
├─────────────────────────────────────────┤
│ - reasons: String[]                     │
│ - initialized: boolean                  │
│ - lastAccess: LocalDateTime             │
├─────────────────────────────────────────┤
│ + initializeReasons(): void             │
│ + getRandomReason(): String             │
│ + getCacheStats(): CacheStats           │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│           RejectionResponse             │
├─────────────────────────────────────────┤
│ - id: int                               │
│ - reason: String                        │
│ - timestamp: LocalDateTime              │
│ - requestId: String                     │
├─────────────────────────────────────────┤
│ + getId(): int                          │
│ + getReason(): String                   │
│ + getTimestamp(): LocalDateTime         │
│ + getRequestId(): String                │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│            SecurityFilter               │
├─────────────────────────────────────────┤
│ - rateLimiter: RateLimiter              │
│ - ipBlocklist: Set<String>              │
│ - requestValidator: RequestValidator    │
├─────────────────────────────────────────┤
│ + doFilter(): void                      │
│ + isRateLimited(): boolean              │
│ + validateRequest(): boolean            │
└─────────────────────────────────────────┘
```

## Sequence Diagram - Request Flow

```
Client    Controller    Service    Cache    Security    Metrics
  │           │           │         │         │           │
  │ GET /api/v1/rejection │         │         │           │
  ├──────────►│           │         │         │           │
  │           │ validate  │         │         │           │
  │           ├──────────────────────────────►│           │
  │           │ ◄─────────────────────────────┤           │
  │           │ rate check│         │         │           │
  │           ├──────────────────────────────►│           │
  │           │ ◄─────────────────────────────┤           │
  │           │ getRandom │         │         │           │
  │           ├──────────►│         │         │           │
  │           │           │ getReason│        │           │
  │           │           ├────────►│         │           │
  │           │           │ reason  │         │           │
  │           │           │◄────────┤         │           │
  │           │           │ createResponse     │           │
  │           │           ├─────────┐│        │           │
  │           │           │◄────────┘│        │           │
  │           │ response  │         │         │           │
  │           │◄──────────┤         │         │           │
  │           │ record    │         │         │           │
  │           ├─────────────────────────────────────────►│
  │           │ JSON      │         │         │           │
  │◄──────────┤           │         │         │           │
```

## Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                       │
│                                                                 │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────┐  │
│  │   Web Layer     │    │  Security Layer │    │ Monitoring  │  │
│  │                 │    │                 │    │   Layer     │  │
│  │ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────┐ │  │
│  │ │ Controller  │ │    │ │Rate Limiter │ │    │ │Actuator │ │  │
│  │ └─────────────┘ │    │ └─────────────┘ │    │ └─────────┘ │  │
│  │ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────┐ │  │
│  │ │CORS Config  │ │    │ │   Filters   │ │    │ │Metrics  │ │  │
│  │ └─────────────┘ │    │ └─────────────┘ │    │ └─────────┘ │  │
│  └─────────────────┘    └─────────────────┘    └─────────────┘  │
│                                                                 │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────┐  │
│  │ Business Layer  │    │   Data Layer    │    │Config Layer │  │
│  │                 │    │                 │    │             │  │
│  │ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────┐ │  │
│  │ │   Service   │ │    │ │   Cache     │ │    │ │Security │ │  │
│  │ └─────────────┘ │    │ └─────────────┘ │    │ └─────────┘ │  │
│  │ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────┐ │  │
│  │ │ Validation  │ │    │ │Random Gen   │ │    │ │  JVM    │ │  │
│  │ └─────────────┘ │    │ └─────────────┘ │    │ └─────────┘ │  │
│  └─────────────────┘    └─────────────────┘    └─────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Deployment Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Cloud Platform                           │
│                     (Railway/Render)                            │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                   Container Instance                        ││
│  │                                                             ││
│  │  ┌─────────────────────────────────────────────────────────┐││
│  │  │                  JVM Process                            │││
│  │  │                                                         │││
│  │  │  ┌─────────────────────────────────────────────────────┐│││
│  │  │  │            Spring Boot App                          ││││
│  │  │  │                                                     ││││
│  │  │  │  Heap Memory: 1GB                                  ││││
│  │  │  │  - Eden Space: 256MB                               ││││
│  │  │  │  - Survivor Space: 64MB                            ││││
│  │  │  │  - Old Generation: 680MB                           ││││
│  │  │  │                                                     ││││
│  │  │  │  Thread Pool: 200 threads                          ││││
│  │  │  │  Connection Pool: 20,000 connections               ││││
│  │  │  └─────────────────────────────────────────────────────┘│││
│  │  └─────────────────────────────────────────────────────────┘││
│  │                                                             ││
│  │  CPU: 1-2 vCPUs                                            ││
│  │  RAM: 2GB                                                  ││
│  │  Network: 100Mbps                                          ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                 │
│  Load Balancer (Platform Managed)                              │
│  SSL Termination (Platform Managed)                            │
│  Auto-scaling (Platform Managed)                               │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                         CDN Layer                               │
│                    (Cloudflare Free)                            │
│                                                                 │
│  Edge Locations: Global                                         │
│  Cache TTL: 1 hour                                             │
│  DDoS Protection: Enabled                                       │
│  SSL: Enabled                                                   │
└─────────────────────────────────────────────────────────────────┘
```

## State Diagram - Application Lifecycle

```
┌─────────────┐
│   STARTING  │
└──────┬──────┘
       │ Initialize Cache
       ▼
┌─────────────┐
│ INITIALIZING│
└──────┬──────┘
       │ Load 100 Reasons
       ▼
┌─────────────┐     Rate Limit     ┌─────────────┐
│   RUNNING   │◄──── Exceeded ────►│  THROTTLED  │
└──────┬──────┘                    └──────┬──────┘
       │                                  │
       │ Health Check Failed              │ Rate Limit Reset
       ▼                                  ▼
┌─────────────┐                    ┌─────────────┐
│  DEGRADED   │                    │   RUNNING   │
└──────┬──────┘                    └─────────────┘
       │ Critical Error
       ▼
┌─────────────┐
│   STOPPED   │
└─────────────┘
```

## Activity Diagram - Request Processing

```
Start
  │
  ▼
┌─────────────────┐
│ Receive Request │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐      No      ┌─────────────────┐
│ Validate Headers├─────────────►│ Return 400 Bad  │
└─────────┬───────┘              │ Request         │
          │ Yes                  └─────────────────┘
          ▼
┌─────────────────┐      No      ┌─────────────────┐
│ Check Rate Limit├─────────────►│ Return 429 Too  │
└─────────┬───────┘              │ Many Requests   │
          │ Yes                  └─────────────────┘
          ▼
┌─────────────────┐
│ Generate Random │
│ Index (0-99)    │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│ Get Reason from │
│ Cache Array     │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│ Create Response │
│ Object          │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│ Record Metrics  │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│ Return JSON     │
│ Response        │
└─────────┬───────┘
          │
          ▼
        End
```

This comprehensive UML documentation provides a complete visual representation of the system architecture, ensuring clear understanding of component interactions, data flow, and system behavior under various conditions.