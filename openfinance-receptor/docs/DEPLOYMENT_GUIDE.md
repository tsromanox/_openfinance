# OpenFinance Receptor - Deployment Guide

## Overview

This guide covers deployment strategies for the OpenFinance Receptor platform across different environments, from development to production. The platform is designed for high-performance processing with Java 21 Virtual Threads and requires specific configuration for optimal operation.

## Prerequisites

### System Requirements

#### Minimum Requirements
- **CPU**: 4 cores (8+ recommended for production)
- **Memory**: 4GB RAM (8-16GB recommended for production)
- **Storage**: 20GB available space
- **Network**: 1Gbps network interface
- **OS**: Linux (Ubuntu 20.04+, RHEL 8+, or similar)

#### Production Requirements
- **CPU**: 8+ cores with AVX2 support
- **Memory**: 16-32GB RAM
- **Storage**: SSD with 100GB+ available space
- **Network**: 10Gbps network interface
- **Load Balancer**: For multi-instance deployments

#### Java Requirements
- **Java Version**: OpenJDK 21+ with Virtual Thread support
- **JVM**: OpenJDK, Oracle JDK, or Azul Zulu 21+
- **GC**: ZGC or G1GC recommended for low-latency operations

### Infrastructure Dependencies

#### Required Services
- **PostgreSQL 16+**: Primary database
- **Redis 7+**: Caching and session management (optional)
- **Kafka 3.5+**: Event streaming (optional)

#### Monitoring Stack
- **Prometheus**: Metrics collection
- **Grafana**: Dashboards and visualization
- **Jaeger/Zipkin**: Distributed tracing (optional)

## Local Development Deployment

### Docker Compose Setup

#### 1. Infrastructure Services
```yaml
# docker/docker-compose.dev.yml
version: '3.8'

services:
  postgres:
    image: postgres:16
    container_name: openfinance-postgres
    environment:
      POSTGRES_DB: openfinance_receptor_dev
      POSTGRES_USER: openfinance_dev
      POSTGRES_PASSWORD: openfinance_dev
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U openfinance_dev"]
      interval: 30s
      timeout: 10s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: openfinance-redis
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 30s
      timeout: 10s
      retries: 5

  prometheus:
    image: prom/prometheus:latest
    container_name: openfinance-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'

  grafana:
    image: grafana/grafana:latest
    container_name: openfinance-grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources

volumes:
  postgres_data:
  grafana_data:
```

#### 2. Start Infrastructure
```bash
# Start all infrastructure services
docker-compose -f docker/docker-compose.dev.yml up -d

# Check services status
docker-compose -f docker/docker-compose.dev.yml ps

# View logs
docker-compose -f docker/docker-compose.dev.yml logs -f postgres
```

#### 3. Build and Run Application
```bash
# Build application
mvn clean install -DskipTests

# Run with development profile
cd bootstrap
mvn spring-boot:run -Dspring.profiles.active=development

# Or run with specific JVM options
mvn spring-boot:run \
  -Dspring.profiles.active=development \
  -Dspring-boot.run.jvmArguments="\
    --enable-preview \
    -XX:+EnableVirtualThreads \
    -XX:+UseZGC \
    -Xmx4g \
    -Xms1g"
```

#### 4. Verification
```bash
# Health check
curl http://localhost:8080/actuator/health

# Resource endpoint
curl http://localhost:8080/api/v1/resources/health

# Metrics
curl http://localhost:8080/actuator/prometheus

# Access Grafana: http://localhost:3000 (admin/admin)
# Access Prometheus: http://localhost:9090
```

## Container Deployment

### Docker Image Build

#### 1. Dockerfile
```dockerfile
# Multi-stage build for optimal image size
FROM openjdk:21-jdk-slim as builder

WORKDIR /app

# Copy Maven files for dependency resolution
COPY pom.xml .
COPY core/pom.xml core/
COPY modules/pom.xml modules/
COPY infrastructure/pom.xml infrastructure/
COPY bootstrap/pom.xml bootstrap/

# Copy source code
COPY . .

# Build application
RUN ./mvnw clean package -DskipTests

# Runtime image
FROM openjdk:21-jdk-slim

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    jq \
    && rm -rf /var/lib/apt/lists/*

# Create application user
RUN groupadd -g 1001 openfinance && \
    useradd -r -u 1001 -g openfinance openfinance

WORKDIR /app

# Copy application jar
COPY --from=builder /app/bootstrap/target/openfinance-receptor.jar app.jar

# Set ownership
RUN chown -R openfinance:openfinance /app

# Switch to non-root user
USER openfinance

# JVM configuration for containers
ENV JVM_OPTS="-XX:+UseContainerSupport \
              -XX:+UseZGC \
              -XX:+EnableVirtualThreads \
              --enable-preview \
              -Xmx2g \
              -Xms512m \
              -XX:+UnlockExperimentalVMOptions"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar app.jar"]
```

#### 2. Build Image
```bash
# Build with Maven plugin (recommended)
mvn spring-boot:build-image -Pproduction

# Or build with Docker
docker build -t openfinance-receptor:1.0.0 .

# Tag for registry
docker tag openfinance-receptor:1.0.0 your-registry.com/openfinance-receptor:1.0.0

# Push to registry
docker push your-registry.com/openfinance-receptor:1.0.0
```

#### 3. Run Container
```bash
# Run with development profile
docker run -d \
  --name openfinance-receptor \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=development \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/openfinance_receptor_dev \
  -e DATABASE_USERNAME=openfinance_dev \
  -e DATABASE_PASSWORD=openfinance_dev \
  openfinance-receptor:1.0.0

# Run with production settings
docker run -d \
  --name openfinance-receptor-prod \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e DATABASE_URL=${DATABASE_URL} \
  -e DATABASE_USERNAME=${DATABASE_USERNAME} \
  -e DATABASE_PASSWORD=${DATABASE_PASSWORD} \
  -e JVM_OPTS="-Xmx8g -Xms2g" \
  --memory=10g \
  --cpus=4 \
  openfinance-receptor:1.0.0
```

## Kubernetes Deployment

### Namespace and Resources

#### 1. Namespace
```yaml
# k8s/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: openfinance-receptor
  labels:
    name: openfinance-receptor
    environment: production
```

#### 2. ConfigMap
```yaml
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: openfinance-receptor-config
  namespace: openfinance-receptor
data:
  application.yml: |
    spring:
      profiles:
        active: production
      datasource:
        url: jdbc:postgresql://postgres-service:5432/openfinance_receptor_prod
        hikari:
          maximum-pool-size: 100
          minimum-idle: 20
    
    openfinance:
      resources:
        virtual-threads:
          max-pool-size: 25000
        batch:
          size: 1000
          max-concurrent: 500
        adaptive:
          memory-threshold: 0.90
          cpu-threshold: 0.85
    
    management:
      endpoints:
        web:
          exposure:
            include: "health,metrics,prometheus,info"
      metrics:
        export:
          prometheus:
            enabled: true
```

#### 3. Secret
```yaml
# k8s/secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: openfinance-receptor-secret
  namespace: openfinance-receptor
type: Opaque
stringData:
  DATABASE_USERNAME: openfinance_prod
  DATABASE_PASSWORD: your-secure-password
  ADMIN_USERNAME: admin
  ADMIN_PASSWORD: your-admin-password
```

#### 4. Deployment
```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: openfinance-receptor
  namespace: openfinance-receptor
  labels:
    app: openfinance-receptor
spec:
  replicas: 3
  selector:
    matchLabels:
      app: openfinance-receptor
  template:
    metadata:
      labels:
        app: openfinance-receptor
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      containers:
      - name: openfinance-receptor
        image: your-registry.com/openfinance-receptor:1.0.0
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DATABASE_USERNAME
          valueFrom:
            secretKeyRef:
              name: openfinance-receptor-secret
              key: DATABASE_USERNAME
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: openfinance-receptor-secret
              key: DATABASE_PASSWORD
        - name: JVM_OPTS
          value: >-
            -XX:+UseContainerSupport
            -XX:+UseZGC
            -XX:+EnableVirtualThreads
            --enable-preview
            -Xmx6g
            -Xms2g
            -XX:+UnlockExperimentalVMOptions
        resources:
          requests:
            memory: "4Gi"
            cpu: "2"
          limits:
            memory: "8Gi"
            cpu: "4"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 45
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
        - name: logs-volume
          mountPath: /app/logs
      volumes:
      - name: config-volume
        configMap:
          name: openfinance-receptor-config
      - name: logs-volume
        emptyDir: {}
      restartPolicy: Always
      terminationGracePeriodSeconds: 30
```

#### 5. Service
```yaml
# k8s/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: openfinance-receptor-service
  namespace: openfinance-receptor
  labels:
    app: openfinance-receptor
spec:
  type: ClusterIP
  ports:
  - port: 8080
    targetPort: 8080
    name: http
  selector:
    app: openfinance-receptor
```

#### 6. Ingress
```yaml
# k8s/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: openfinance-receptor-ingress
  namespace: openfinance-receptor
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
    nginx.ingress.kubernetes.io/proxy-connect-timeout: "30"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "60"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "60"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
  - hosts:
    - openfinance-receptor.yourdomain.com
    secretName: openfinance-receptor-tls
  rules:
  - host: openfinance-receptor.yourdomain.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: openfinance-receptor-service
            port:
              number: 8080
```

#### 7. HorizontalPodAutoscaler
```yaml
# k8s/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: openfinance-receptor-hpa
  namespace: openfinance-receptor
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: openfinance-receptor
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  - type: Pods
    pods:
      metric:
        name: openfinance_resources_throughput
      target:
        type: AverageValue
        averageValue: "100"
```

### Deploy to Kubernetes

```bash
# Apply all configurations
kubectl apply -f k8s/

# Check deployment status
kubectl get deployments -n openfinance-receptor
kubectl get pods -n openfinance-receptor
kubectl get services -n openfinance-receptor

# Check logs
kubectl logs -f deployment/openfinance-receptor -n openfinance-receptor

# Check autoscaling
kubectl get hpa -n openfinance-receptor

# Port forward for testing
kubectl port-forward -n openfinance-receptor service/openfinance-receptor-service 8080:8080
```

## Production Deployment

### Environment Configuration

#### 1. Production application.yml
```yaml
# Create production-specific configuration
spring:
  profiles:
    active: production
  
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://prod-db:5432/openfinance_receptor_prod}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 100
      minimum-idle: 20
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
      leak-detection-threshold: 60000

  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc:
          batch_size: 100

openfinance:
  virtual-threads:
    enabled: true
    max-pool-size: 25000
    
  resources:
    enabled: true
    batch:
      size: 1000
      max-concurrent: 500
    adaptive:
      memory-threshold: 0.90
      cpu-threshold: 0.85
    monitoring:
      performance-log-interval: 900s
      detailed-metrics: false

# Security
spring:
  security:
    user:
      name: ${ADMIN_USERNAME}
      password: ${ADMIN_PASSWORD}

# Monitoring
management:
  endpoints:
    web:
      exposure:
        include: "health,metrics,prometheus,info"
  metrics:
    export:
      prometheus:
        enabled: true
        step: 30s

# Logging
logging:
  level:
    br.com.openfinance: INFO
    org.springframework: WARN
    java.util.concurrent: ERROR
  file:
    name: /var/log/openfinance-receptor/application.log
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30
      total-size-cap: 3GB
```

#### 2. JVM Configuration
```bash
#!/bin/bash
# start-production.sh

export JAVA_OPTS="
  --enable-preview
  -XX:+UseZGC
  -XX:+EnableVirtualThreads
  -XX:+UseContainerSupport
  -Xmx8g
  -Xms2g
  -XX:+UnlockExperimentalVMOptions
  -XX:+UnlockDiagnosticVMOptions
  -XX:+LogVMOutput
  -XX:+UseStringDeduplication
  -XX:+OptimizeStringConcat
  -Djdk.virtualThreadScheduler.parallelism=$(nproc)
  -Djdk.virtualThreadScheduler.maxPoolSize=10000
  -Djava.util.concurrent.ForkJoinPool.common.parallelism=$(($(nproc) * 2))
"

export SPRING_PROFILES_ACTIVE=production
export DATABASE_URL=jdbc:postgresql://prod-db:5432/openfinance_receptor_prod
export DATABASE_USERNAME=openfinance_prod
export DATABASE_PASSWORD=${DB_PASSWORD}
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD=${ADMIN_PASSWORD}

java $JAVA_OPTS -jar /app/openfinance-receptor.jar
```

### Database Setup

#### 1. PostgreSQL Configuration
```sql
-- Create production database
CREATE DATABASE openfinance_receptor_prod;
CREATE USER openfinance_prod WITH PASSWORD 'secure-password';
GRANT ALL PRIVILEGES ON DATABASE openfinance_receptor_prod TO openfinance_prod;

-- Performance tuning
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '1GB';
ALTER SYSTEM SET maintenance_work_mem = '64MB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET wal_buffers = '16MB';
ALTER SYSTEM SET default_statistics_target = 100;
ALTER SYSTEM SET random_page_cost = 1.1;
ALTER SYSTEM SET effective_io_concurrency = 200;

-- Reload configuration
SELECT pg_reload_conf();
```

#### 2. Connection Pooling with PgBouncer
```ini
# /etc/pgbouncer/pgbouncer.ini
[databases]
openfinance_receptor_prod = host=localhost port=5432 dbname=openfinance_receptor_prod

[pgbouncer]
listen_port = 6432
listen_addr = *
auth_type = md5
auth_file = /etc/pgbouncer/userlist.txt
logfile = /var/log/pgbouncer/pgbouncer.log
pidfile = /var/run/pgbouncer/pgbouncer.pid
admin_users = pgbouncer
pool_mode = transaction
server_reset_query = DISCARD ALL
max_client_conn = 100
default_pool_size = 30
reserve_pool_size = 10
reserve_pool_timeout = 5
max_db_connections = 50
```

### Monitoring Setup

#### 1. Prometheus Configuration
```yaml
# monitoring/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "openfinance-rules.yml"

scrape_configs:
  - job_name: 'openfinance-receptor'
    static_configs:
      - targets: ['openfinance-receptor:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s
    scrape_timeout: 10s
    
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']
      
  - job_name: 'jvm'
    static_configs:
      - targets: ['openfinance-receptor:8080']
    metrics_path: '/actuator/prometheus'
    params:
      'match[]': ['jvm_*']
```

#### 2. Alert Rules
```yaml
# monitoring/openfinance-rules.yml
groups:
  - name: openfinance-receptor
    rules:
      - alert: HighCPUUsage
        expr: openfinance_resources_cpu_usage > 0.90
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage on OpenFinance Receptor"
          description: "CPU usage is above 90% for more than 5 minutes"
          
      - alert: HighMemoryUsage
        expr: openfinance_resources_memory_usage > 0.90
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage on OpenFinance Receptor"
          
      - alert: HighErrorRate
        expr: rate(openfinance_resources_errors_total[5m]) > 0.1
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate in resource processing"
          
      - alert: LowThroughput
        expr: openfinance_resources_throughput < 10
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Low resource processing throughput"
          
      - alert: ApplicationDown
        expr: up{job="openfinance-receptor"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "OpenFinance Receptor is down"
```

#### 3. Grafana Dashboard
```json
{
  "dashboard": {
    "id": null,
    "title": "OpenFinance Receptor Dashboard",
    "tags": ["openfinance", "virtual-threads"],
    "timezone": "browser",
    "panels": [
      {
        "title": "Resource Processing Throughput",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(openfinance_resources_discovered_total[5m])",
            "legendFormat": "Discovery Rate"
          },
          {
            "expr": "rate(openfinance_resources_synced_total[5m])",
            "legendFormat": "Sync Rate"
          }
        ]
      },
      {
        "title": "Virtual Thread Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "openfinance_resources_virtual_threads_active",
            "legendFormat": "Active Virtual Threads"
          },
          {
            "expr": "openfinance_resources_operations_concurrent",
            "legendFormat": "Concurrent Operations"
          }
        ]
      },
      {
        "title": "System Resources",
        "type": "graph",
        "targets": [
          {
            "expr": "openfinance_resources_cpu_usage * 100",
            "legendFormat": "CPU Usage (%)"
          },
          {
            "expr": "openfinance_resources_memory_usage * 100",
            "legendFormat": "Memory Usage (%)"
          }
        ]
      }
    ]
  }
}
```

### Security Configuration

#### 1. SSL/TLS Setup
```bash
# Generate SSL certificate
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes

# Configure SSL in application.yml
server:
  port: 8443
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: your-keystore-password
    key-store-type: PKCS12
    key-alias: openfinance-receptor
```

#### 2. Firewall Configuration
```bash
# UFW firewall rules
sudo ufw allow 22/tcp   # SSH
sudo ufw allow 8080/tcp # HTTP
sudo ufw allow 8443/tcp # HTTPS
sudo ufw allow from 10.0.0.0/8 to any port 5432  # PostgreSQL internal
sudo ufw enable
```

#### 3. Network Security
```yaml
# Network policies for Kubernetes
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: openfinance-receptor-netpol
  namespace: openfinance-receptor
spec:
  podSelector:
    matchLabels:
      app: openfinance-receptor
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - namespaceSelector:
        matchLabels:
          name: database
    ports:
    - protocol: TCP
      port: 5432
```

## Performance Tuning

### JVM Tuning

#### Production JVM Settings
```bash
export JAVA_OPTS="
  # Virtual Threads and Preview Features
  --enable-preview
  -XX:+EnableVirtualThreads
  -XX:+UnlockExperimentalVMOptions
  
  # Garbage Collection
  -XX:+UseZGC
  -XX:+UseLargePages
  -XX:+UseTransparentHugePages
  
  # Memory Settings
  -Xmx16g
  -Xms4g
  -XX:MaxDirectMemorySize=2g
  
  # Performance Optimizations
  -XX:+UseStringDeduplication
  -XX:+OptimizeStringConcat
  -XX:+UseCompressedOops
  -XX:+UseCompressedClassPointers
  
  # Monitoring and Debugging
  -XX:+FlightRecorder
  -XX:StartFlightRecording=duration=1h,filename=openfinance-$(date +%Y%m%d-%H%M%S).jfr
  -XX:+UnlockDiagnosticVMOptions
  -XX:+LogVMOutput
  
  # Virtual Thread Scheduler
  -Djdk.virtualThreadScheduler.parallelism=$(nproc)
  -Djdk.virtualThreadScheduler.maxPoolSize=25000
  -Djava.util.concurrent.ForkJoinPool.common.parallelism=$(($(nproc) * 2))
"
```

### Database Tuning

#### PostgreSQL Optimization
```sql
-- Connection and memory settings
ALTER SYSTEM SET max_connections = 200;
ALTER SYSTEM SET shared_buffers = '4GB';
ALTER SYSTEM SET effective_cache_size = '12GB';
ALTER SYSTEM SET work_mem = '256MB';
ALTER SYSTEM SET maintenance_work_mem = '1GB';

-- WAL and checkpoint settings
ALTER SYSTEM SET wal_buffers = '64MB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET checkpoint_timeout = '15min';
ALTER SYSTEM SET max_wal_size = '4GB';
ALTER SYSTEM SET min_wal_size = '1GB';

-- Query performance
ALTER SYSTEM SET effective_io_concurrency = 200;
ALTER SYSTEM SET random_page_cost = 1.1;
ALTER SYSTEM SET seq_page_cost = 1.0;

-- Reload configuration
SELECT pg_reload_conf();
```

### Application Tuning

#### Resource Processing Configuration
```yaml
openfinance:
  resources:
    virtual-threads:
      max-pool-size: 25000
    
    batch:
      size: 2000
      max-concurrent: 1000
      parallel-factor: 50
    
    adaptive:
      enabled: true
      batch-size:
        min: 500
        max: 5000
      concurrency:
        min: 200
        max: 2000
      memory-threshold: 0.85
      cpu-threshold: 0.80
      interval:
        min: 15000   # 15 seconds
        max: 300000  # 5 minutes
        
    timeout:
      api-call: 30s
      discovery: 60s
      sync: 120s
      validation: 45s
      monitoring: 30s
```

## Backup and Recovery

### Database Backup
```bash
#!/bin/bash
# backup-database.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/var/backups/openfinance-receptor"
DB_NAME="openfinance_receptor_prod"

# Create backup directory
mkdir -p $BACKUP_DIR

# Full database backup
pg_dump -h localhost -U openfinance_prod -d $DB_NAME \
  --verbose --no-owner --no-privileges \
  --format=custom \
  --file=$BACKUP_DIR/full_backup_$DATE.dump

# Compress backup
gzip $BACKUP_DIR/full_backup_$DATE.dump

# Remove old backups (keep last 7 days)
find $BACKUP_DIR -name "*.gz" -mtime +7 -delete

echo "Backup completed: $BACKUP_DIR/full_backup_$DATE.dump.gz"
```

### Application State Backup
```bash
#!/bin/bash
# backup-application.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/var/backups/openfinance-receptor"

# Backup configuration
kubectl get configmap openfinance-receptor-config -o yaml > $BACKUP_DIR/config_$DATE.yaml
kubectl get secret openfinance-receptor-secret -o yaml > $BACKUP_DIR/secret_$DATE.yaml

# Backup logs
tar -czf $BACKUP_DIR/logs_$DATE.tar.gz /var/log/openfinance-receptor/

# Backup metrics data (if using local storage)
tar -czf $BACKUP_DIR/metrics_$DATE.tar.gz /var/lib/prometheus/
```

### Disaster Recovery

#### Database Recovery
```bash
#!/bin/bash
# restore-database.sh

BACKUP_FILE=$1

if [ -z "$BACKUP_FILE" ]; then
    echo "Usage: $0 <backup_file>"
    exit 1
fi

# Stop application
kubectl scale deployment openfinance-receptor --replicas=0

# Restore database
gunzip -c $BACKUP_FILE | pg_restore -h localhost -U openfinance_prod \
  -d openfinance_receptor_prod --clean --if-exists

# Start application
kubectl scale deployment openfinance-receptor --replicas=3

echo "Database restored from $BACKUP_FILE"
```

## Troubleshooting

### Common Issues

#### Virtual Threads Not Working
```bash
# Check Java version
java --version | grep -i virtual

# Verify JVM flags
ps aux | grep java | grep -o -- '--enable-preview\|EnableVirtualThreads'

# Check application logs
kubectl logs -f deployment/openfinance-receptor | grep -i "virtual"
```

#### Memory Issues
```bash
# Check memory usage
curl localhost:8080/actuator/metrics/jvm.memory.used

# Generate heap dump
kubectl exec -it openfinance-receptor-pod -- jcmd 1 GC.run_finalization
kubectl exec -it openfinance-receptor-pod -- jcmd 1 VM.gc
```

#### Performance Issues
```bash
# Check resource utilization
curl localhost:8080/api/v1/resources/utilization

# Monitor Virtual Thread usage
curl localhost:8080/actuator/threaddump | jq '.threads[] | select(.threadName | contains("virtual"))'

# Check adaptive settings
curl localhost:8080/api/v1/resources/metrics/performance
```

#### Database Connection Issues
```bash
# Test database connectivity
kubectl exec -it openfinance-receptor-pod -- nc -zv postgres-service 5432

# Check connection pool metrics
curl localhost:8080/actuator/metrics/hikaricp.connections.active
```

### Log Analysis

#### Application Logs
```bash
# Real-time log monitoring
kubectl logs -f deployment/openfinance-receptor --all-containers

# Search for specific patterns
kubectl logs deployment/openfinance-receptor | grep -E "(ERROR|WARN|Virtual)"

# Resource processing logs
kubectl logs deployment/openfinance-receptor | grep "resource-discovery\|resource-sync"
```

#### Performance Analysis
```bash
# JFR analysis
jfr summary performance.jfr
jfr print --events jdk.VirtualThreadStart,jdk.VirtualThreadEnd performance.jfr

# Thread dump analysis
kubectl exec openfinance-receptor-pod -- jstack 1 > threaddump.txt
grep -A 10 -B 5 "VirtualThread" threaddump.txt
```

This deployment guide provides comprehensive instructions for deploying the OpenFinance Receptor platform across various environments with proper configuration, monitoring, and operational procedures.