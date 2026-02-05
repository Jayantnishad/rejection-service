# Multi-stage build for optimized production image
FROM eclipse-temurin:17-jdk-alpine as builder

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY src ./src
COPY mvnw .
COPY .mvn .mvn

# Build application
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Production image with security hardening
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create non-root user for security
RUN addgroup -g 1001 -S spring && adduser -u 1001 -S spring -G spring
USER spring:spring

# Expose port
EXPOSE 8080

# Health check for container orchestration
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/health || exit 1

# Production JVM optimization
ENTRYPOINT ["java", \
    "-Xms512m", \
    "-Xmx1g", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=100", \
    "-XX:+UseStringDeduplication", \
    "-Djava.awt.headless=true", \
    "-Dspring.profiles.active=prod", \
    "-jar", \
    "app.jar"]