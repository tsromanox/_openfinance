FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app

COPY target/accounts-service-1.0.0.jar app.jar

RUN addgroup -g 1000 spring && adduser -u 1000 -G spring -s /bin/sh -D spring
USER spring:spring

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Enable preview features and Virtual Threads optimizations
ENTRYPOINT ["java", \
  "--enable-preview", \
  "-XX:+UseZGC", \
  "-XX:+ZGenerational", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djdk.virtualThreadScheduler.parallelism=10000", \
  "-Djdk.virtualThreadScheduler.maxPoolSize=100000", \
  "-Djdk.tracePinnedThreads=short", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", \
  "app.jar"]