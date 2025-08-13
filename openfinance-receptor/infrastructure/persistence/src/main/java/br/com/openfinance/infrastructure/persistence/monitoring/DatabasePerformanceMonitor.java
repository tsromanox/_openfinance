package br.com.openfinance.infrastructure.persistence.monitoring;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Advanced database performance monitoring with Virtual Thread and connection pool metrics.
 */
@Slf4j
@Component
public class DatabasePerformanceMonitor {
    
    private final DataSource primaryDataSource;
    private final DataSource virtualThreadDataSource;
    private final MeterRegistry meterRegistry;
    
    // Performance metrics
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final DoubleAdder averageQueryTime = new DoubleAdder();
    private final AtomicLong slowQueries = new AtomicLong(0);
    private final AtomicLong connectionErrors = new AtomicLong(0);
    
    // Database statistics
    private volatile long databaseSize = 0;
    private volatile long activeTransactions = 0;
    private volatile long totalConnections = 0;
    private volatile double cacheHitRatio = 0.0;
    
    public DatabasePerformanceMonitor(
            @Qualifier("primaryDataSource") DataSource primaryDataSource,
            @Qualifier("virtualThreadDataSource") DataSource virtualThreadDataSource,
            MeterRegistry meterRegistry) {
        
        this.primaryDataSource = primaryDataSource;
        this.virtualThreadDataSource = virtualThreadDataSource;
        this.meterRegistry = meterRegistry;
        
        initializeMetrics();
    }
    
    private void initializeMetrics() {
        // Connection pool metrics
        if (primaryDataSource instanceof HikariDataSource hikariDataSource) {
            HikariPoolMXBean poolBean = hikariDataSource.getHikariPoolMXBean();
            
            Gauge.builder("database.pool.active.connections")
                    .description("Number of active database connections")
                    .register(meterRegistry, poolBean, HikariPoolMXBean::getActiveConnections);
            
            Gauge.builder("database.pool.idle.connections")
                    .description("Number of idle database connections")
                    .register(meterRegistry, poolBean, HikariPoolMXBean::getIdleConnections);
            
            Gauge.builder("database.pool.total.connections")
                    .description("Total number of database connections")
                    .register(meterRegistry, poolBean, HikariPoolMXBean::getTotalConnections);
            
            Gauge.builder("database.pool.threads.awaiting")
                    .description("Number of threads awaiting database connections")
                    .register(meterRegistry, poolBean, HikariPoolMXBean::getThreadsAwaitingConnection);
        }
        
        // Virtual Thread specific metrics
        if (virtualThreadDataSource instanceof HikariDataSource vtHikariDataSource) {
            HikariPoolMXBean vtPoolBean = vtHikariDataSource.getHikariPoolMXBean();
            
            Gauge.builder("database.pool.virtual.threads.active")
                    .description("Active connections in Virtual Thread pool")
                    .register(meterRegistry, vtPoolBean, HikariPoolMXBean::getActiveConnections);
            
            Gauge.builder("database.pool.virtual.threads.total")
                    .description("Total connections in Virtual Thread pool")
                    .register(meterRegistry, vtPoolBean, HikariPoolMXBean::getTotalConnections);
        }
        
        // Custom metrics
        Gauge.builder("database.size.bytes")
                .description("Total database size in bytes")
                .register(meterRegistry, this, monitor -> databaseSize);
        
        Gauge.builder("database.transactions.active")
                .description("Number of active database transactions")
                .register(meterRegistry, this, monitor -> activeTransactions);
        
        Gauge.builder("database.cache.hit.ratio")
                .description("Database cache hit ratio")
                .register(meterRegistry, this, monitor -> cacheHitRatio);
        
        Gauge.builder("database.queries.total")
                .description("Total number of executed queries")
                .register(meterRegistry, this, monitor -> totalQueries.get());
        
        Gauge.builder("database.queries.slow")
                .description("Number of slow queries")
                .register(meterRegistry, this, monitor -> slowQueries.get());
        
        log.info("Database performance monitoring initialized");
    }
    
    /**
     * Monitor connection pool health and performance.
     */
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void monitorConnectionPools() {
        try {
            monitorHikariPool("primary", primaryDataSource);
            monitorHikariPool("virtual-thread", virtualThreadDataSource);
            
        } catch (Exception e) {
            log.error("Error monitoring connection pools: {}", e.getMessage(), e);
            connectionErrors.incrementAndGet();
        }
    }
    
    private void monitorHikariPool(String poolName, DataSource dataSource) {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            HikariPoolMXBean poolBean = hikariDataSource.getHikariPoolMXBean();
            
            int active = poolBean.getActiveConnections();
            int idle = poolBean.getIdleConnections();
            int total = poolBean.getTotalConnections();
            int awaiting = poolBean.getThreadsAwaitingConnection();
            
            log.debug("Pool {}: Active={}, Idle={}, Total={}, Awaiting={}", 
                     poolName, active, idle, total, awaiting);
            
            // Alert if pool utilization is high
            if (total > 0 && (double) active / total > 0.8) {
                log.warn("High connection pool utilization in {}: {}/{} ({}%)", 
                        poolName, active, total, (active * 100 / total));
            }
            
            // Alert if threads are waiting
            if (awaiting > 0) {
                log.warn("Threads awaiting connections in {}: {}", poolName, awaiting);
            }
        }
    }
    
    /**
     * Collect detailed database statistics.
     */
    @Scheduled(fixedDelay = 60000) // Every minute
    public void collectDatabaseStatistics() {
        Timer.Sample sample = Timer.Sample.start(meterRegistry);
        
        try (Connection connection = primaryDataSource.getConnection()) {
            
            // Database size
            collectDatabaseSize(connection);
            
            // Active transactions
            collectActiveTransactions(connection);
            
            // Cache hit ratio
            collectCacheHitRatio(connection);
            
            // Connection statistics
            collectConnectionStatistics(connection);
            
            // Query performance statistics
            collectQueryStatistics(connection);
            
        } catch (SQLException e) {
            log.error("Error collecting database statistics: {}", e.getMessage(), e);
            connectionErrors.incrementAndGet();
        } finally {
            sample.stop(Timer.builder("database.monitoring.collection.time")
                    .description("Time taken to collect database statistics")
                    .register(meterRegistry));
        }
    }
    
    private void collectDatabaseSize(Connection connection) throws SQLException {
        String sql = """
            SELECT pg_database_size(current_database()) as db_size
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                databaseSize = rs.getLong("db_size");
            }
        }
    }
    
    private void collectActiveTransactions(Connection connection) throws SQLException {
        String sql = """
            SELECT COUNT(*) as active_txns
            FROM pg_stat_activity 
            WHERE state = 'active' AND query NOT LIKE '%pg_stat_activity%'
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                activeTransactions = rs.getLong("active_txns");
            }
        }
    }
    
    private void collectCacheHitRatio(Connection connection) throws SQLException {
        String sql = """
            SELECT 
                CASE 
                    WHEN (blks_hit + blks_read) = 0 THEN 0
                    ELSE ROUND(blks_hit::numeric / (blks_hit + blks_read) * 100, 2)
                END as cache_hit_ratio
            FROM pg_stat_database 
            WHERE datname = current_database()
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                cacheHitRatio = rs.getDouble("cache_hit_ratio");
            }
        }
    }
    
    private void collectConnectionStatistics(Connection connection) throws SQLException {
        String sql = """
            SELECT 
                SUM(numbackends) as total_connections,
                COUNT(*) as databases
            FROM pg_stat_database
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                totalConnections = rs.getLong("total_connections");
            }
        }
    }
    
    private void collectQueryStatistics(Connection connection) throws SQLException {
        String sql = """
            SELECT 
                calls as total_queries,
                mean_exec_time as avg_exec_time,
                COUNT(*) FILTER (WHERE mean_exec_time > 1000) as slow_queries_count
            FROM pg_stat_statements 
            WHERE query NOT LIKE '%pg_stat%'
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            long totalCalls = 0;
            double totalExecTime = 0;
            long slowQueriesCount = 0;
            
            while (rs.next()) {
                totalCalls += rs.getLong("total_queries");
                totalExecTime += rs.getDouble("avg_exec_time");
                slowQueriesCount += rs.getLong("slow_queries_count");
            }
            
            if (totalCalls > 0) {
                totalQueries.set(totalCalls);
                averageQueryTime.reset();
                averageQueryTime.add(totalExecTime / totalCalls);
                slowQueries.set(slowQueriesCount);
            }
        } catch (SQLException e) {
            // pg_stat_statements might not be enabled
            log.debug("Could not collect query statistics (pg_stat_statements may not be enabled): {}", 
                     e.getMessage());
        }
    }
    
    /**
     * Analyze table statistics and provide optimization recommendations.
     */
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void analyzeTablePerformance() {
        try (Connection connection = primaryDataSource.getConnection()) {
            
            String sql = """
                SELECT 
                    schemaname,
                    tablename,
                    n_tup_ins + n_tup_upd + n_tup_del as total_operations,
                    seq_scan,
                    seq_tup_read,
                    idx_scan,
                    idx_tup_fetch
                FROM pg_stat_user_tables 
                WHERE schemaname = 'public'
                ORDER BY total_operations DESC
                LIMIT 10
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    String tableName = rs.getString("tablename");
                    long totalOps = rs.getLong("total_operations");
                    long seqScan = rs.getLong("seq_scan");
                    long seqTupRead = rs.getLong("seq_tup_read");
                    long idxScan = rs.getLong("idx_scan");
                    long idxTupFetch = rs.getLong("idx_tup_fetch");
                    
                    // Check for tables with high sequential scan ratio
                    if (seqScan > 0 && idxScan > 0) {
                        double seqScanRatio = (double) seqScan / (seqScan + idxScan);
                        if (seqScanRatio > 0.5) {
                            log.warn("Table {} has high sequential scan ratio: {:.2f}% (consider adding indexes)", 
                                   tableName, seqScanRatio * 100);
                        }
                    }
                    
                    log.debug("Table {}: Operations={}, SeqScans={}, IdxScans={}", 
                            tableName, totalOps, seqScan, idxScan);
                }
            }
            
        } catch (SQLException e) {
            log.error("Error analyzing table performance: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get comprehensive performance report.
     */
    public DatabasePerformanceReport getPerformanceReport() {
        return new DatabasePerformanceReport(
                LocalDateTime.now(),
                activeConnections.get(),
                totalConnections,
                totalQueries.get(),
                averageQueryTime.doubleValue(),
                slowQueries.get(),
                connectionErrors.get(),
                databaseSize,
                activeTransactions,
                cacheHitRatio
        );
    }
    
    /**
     * Record query execution for metrics.
     */
    public void recordQueryExecution(String queryType, long executionTimeMs) {
        totalQueries.incrementAndGet();
        
        if (executionTimeMs > 1000) { // Slow query threshold: 1 second
            slowQueries.incrementAndGet();
            log.warn("Slow query detected ({}): {}ms", queryType, executionTimeMs);
        }
        
        Timer.builder("database.query.execution.time")
                .tag("type", queryType)
                .description("Database query execution time")
                .register(meterRegistry)
                .record(executionTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    /**
     * Performance report record.
     */
    public record DatabasePerformanceReport(
            LocalDateTime timestamp,
            long activeConnections,
            long totalConnections,
            long totalQueries,
            double averageQueryTimeMs,
            long slowQueries,
            long connectionErrors,
            long databaseSizeBytes,
            long activeTransactions,
            double cacheHitRatio
    ) {
        public long getDatabaseSizeMB() {
            return databaseSizeBytes / (1024 * 1024);
        }
        
        public double getSlowQueryRatio() {
            return totalQueries > 0 ? (double) slowQueries / totalQueries * 100 : 0.0;
        }
    }
}