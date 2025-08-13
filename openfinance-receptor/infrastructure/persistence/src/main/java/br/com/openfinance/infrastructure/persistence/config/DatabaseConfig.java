package br.com.openfinance.infrastructure.persistence.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * Database configuration optimized for Virtual Threads and parallel processing.
 * Supports both traditional JPA and reactive R2DBC for maximum performance.
 */
@Slf4j
@Configuration
@EnableTransactionManagement
@EnableR2dbcRepositories(basePackages = "br.com.openfinance.infrastructure.persistence.repository.reactive")
public class DatabaseConfig extends AbstractR2dbcConfiguration {
    
    @Value("${spring.datasource.url}")
    private String jdbcUrl;
    
    @Value("${spring.datasource.username}")
    private String username;
    
    @Value("${spring.datasource.password}")
    private String password;
    
    @Value("${spring.r2dbc.url}")
    private String r2dbcUrl;
    
    @Value("${openfinance.database.pool.max-size:200}")
    private int maxPoolSize;
    
    @Value("${openfinance.database.pool.min-idle:10}")
    private int minIdle;
    
    @Value("${openfinance.database.pool.max-lifetime:1800000}")
    private long maxLifetime; // 30 minutes
    
    @Value("${openfinance.database.pool.connection-timeout:30000}")
    private long connectionTimeout; // 30 seconds
    
    /**
     * Primary DataSource optimized for Virtual Threads with HikariCP.
     */
    @Primary
    @Bean("primaryDataSource")
    public DataSource primaryDataSource(MeterRegistry meterRegistry) {
        HikariConfig config = new HikariConfig();
        
        // Basic connection settings
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        
        // Virtual Thread optimized pool settings
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setMaxLifetime(maxLifetime);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(600000); // 10 minutes
        config.setValidationTimeout(5000);
        config.setLeakDetectionThreshold(60000);
        
        // Virtual Thread executor for async operations
        config.setScheduledExecutor(Executors.newScheduledThreadPool(1));
        
        // PostgreSQL specific optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "500");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        // Application specific
        config.setPoolName("OpenFinance-HikariCP");
        config.setMetricRegistry(meterRegistry);
        config.setHealthCheckRegistry(null);
        
        log.info("Configured HikariCP with maxPoolSize={}, minIdle={}, maxLifetime={}ms", 
                maxPoolSize, minIdle, maxLifetime);
        
        return new HikariDataSource(config);
    }
    
    /**
     * R2DBC ConnectionFactory for reactive database operations.
     */
    @Override
    @Bean("r2dbcConnectionFactory")
    public ConnectionFactory connectionFactory() {
        // Parse R2DBC URL to extract connection details
        PostgresqlConnectionConfiguration.Builder builder = PostgresqlConnectionConfiguration.builder()
                .host(extractHost(r2dbcUrl))
                .port(extractPort(r2dbcUrl))
                .database(extractDatabase(r2dbcUrl))
                .username(username)
                .password(password)
                .applicationName("openfinance-receptor")
                .connectTimeout(Duration.ofSeconds(30))
                .lockWaitTimeout(Duration.ofSeconds(10))
                .statementTimeout(Duration.ofSeconds(60))
                //.cancelSignalTimeout(Duration.ofSeconds(5))
                // Performance optimizations
                .preparedStatementCacheQueries(500)
                .tcpKeepAlive(true)
                .tcpNoDelay(true);
        
        ConnectionFactory connectionFactory = new PostgresqlConnectionFactory(builder.build());
        
        // Connection pooling for R2DBC
        ConnectionPoolConfiguration poolConfig = ConnectionPoolConfiguration.builder(connectionFactory)
                .maxIdleTime(Duration.ofMinutes(10))
                .maxLifeTime(Duration.ofMinutes(30))
                .maxAcquireTime(Duration.ofSeconds(30))
                .maxCreateConnectionTime(Duration.ofSeconds(30))
                .initialSize(10)
                .maxSize(maxPoolSize)
                .validationQuery("SELECT 1")
                .build();
        
        log.info("Configured R2DBC ConnectionPool with maxSize={}, initialSize={}", 
                maxPoolSize, 10);
        
        return new ConnectionPool(poolConfig);
    }
    
    /**
     * Reactive transaction manager for R2DBC operations.
     */
    @Bean
    public ReactiveTransactionManager reactiveTransactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }
    
    /**
     * DataSource specifically for Virtual Thread operations.
     * Uses separate connection pool to avoid blocking reactive operations.
     */
    @Bean("virtualThreadDataSource")
    @ConditionalOnProperty(name = "openfinance.database.virtual-threads.enabled", havingValue = "true", matchIfMissing = true)
    public DataSource virtualThreadDataSource() {
        HikariConfig config = new HikariConfig();
        
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        
        // Optimized for Virtual Threads - higher pool size, lower timeouts
        config.setMaximumPoolSize(500); // Higher for Virtual Threads
        config.setMinimumIdle(20);
        config.setMaxLifetime(900000); // 15 minutes
        config.setConnectionTimeout(10000); // 10 seconds
        config.setIdleTimeout(300000); // 5 minutes
        config.setValidationTimeout(2000);
        
        // Virtual Thread specific optimizations
        config.addDataSourceProperty("socketTimeout", "10");
        config.addDataSourceProperty("loginTimeout", "5");
        config.addDataSourceProperty("connectTimeout", "10");
        
        config.setPoolName("VirtualThread-HikariCP");
        
        log.info("Configured Virtual Thread DataSource with maxPoolSize={}", 500);
        
        return new HikariDataSource(config);
    }
    
    // Helper methods to parse R2DBC URL
    private String extractHost(String r2dbcUrl) {
        // r2dbc:postgresql://localhost:5432/openfinance
        return r2dbcUrl.split("//")[1].split(":")[0];
    }
    
    private int extractPort(String r2dbcUrl) {
        String[] parts = r2dbcUrl.split("//")[1].split(":");
        return Integer.parseInt(parts[1].split("/")[0]);
    }
    
    private String extractDatabase(String r2dbcUrl) {
        return r2dbcUrl.split("/")[r2dbcUrl.split("/").length - 1];
    }
}