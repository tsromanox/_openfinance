FROM openjdk:21-jdk-slim

LABEL maintainer="OpenFinance Brasil"
LABEL description="OpenFinance Resources Service with Virtual Threads"
LABEL version="1.0.0"

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create app directory
RUN mkdir -p /app
WORKDIR /app

# Copy JAR file
COPY target/openfinance-resources-service-*.jar app.jar

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser
RUN chown -R appuser:appuser /app
USER appuser

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8082/health || exit 1

# JVM arguments optimized for Virtual Threads
ENV JAVA_OPTS="-XX:+UseZGC \
               -XX:+UnlockExperimentalVMOptions \
               --enable-preview \
               -Djdk.virtualThreadScheduler.parallelism=10000 \
               -Djdk.virtualThreadScheduler.maxPoolSize=100000 \
               -Xms2g \
               -Xmx4g \
               -XX:+UseStringDeduplication \
               -XX:+OptimizeStringConcat"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]