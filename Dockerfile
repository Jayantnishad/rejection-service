# Multi-stage build for optimized production image
FROM openjdk:17-jre-slim as builder

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY src ./src

# Build application
RUN apt-get update && apt-get install -y maven
RUN mvn clean package -DskipTests

# Production image with security hardening
FROM openjdk:17-jre-slim

WORKDIR /app

# Copy JAR and static resources from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create non-root user for security
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Expose port
EXPOSE 8080

# Health check for container orchestration
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/health || exit 1

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