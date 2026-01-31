package com.ticketblitz.catalog.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Database configuration
 *
 * CONCURRENCY OPTIMIZATION
 * ========================
 * HikariCP is the fastest JDBC connection pool, but needs tuning
 *
 * FORMULA FOR POOL SIZE
 * =====================
 * connections = ((core_count*2) + effective_spindle_count))
 *
 * for our use case:
 * - 8 cores * 2 = 16
 * - SSD (no spindles) = 1
 * - Optimal pool size = 17 <-> 20
 *
 * WHY NOT BIGGER:
 * - More connections != better performance
 * - Context switching overhead
 * - PostgreSQL has limited connections (typically 100-200)
 * - Leave room for other services
 *
 * READ REPLICA READY:
 * - Architecture supports read replica routing
 * - Writes -> Primary
 * - Reads -> Replica (future enhancement)
 *
 * @author Akhil
 */
@Slf4j
@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    /**
     * primary datasource with optimized hikariCP settings
     */
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        // Connection settings
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");

        // pool sizing
        config.setMaximumPoolSize(20); // based on cores * 2 + spindles
        config.setMinimumIdle(2);

        // connection timeout
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes

        // leak detection (catches connection leaks in dev)
        config.setLeakDetectionThreshold(60000); // 60 seconds

        // performance optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("reWriteBatchInserts", "true");

        // monitoring
        config.setMetricRegistry(null); // TODO: add micrometer metrics
        config.setHealthCheckRegistry(null);

        log.info("HikariCP configured with maxPoolSize={}, minIdle={}",
                config.getMaximumPoolSize(), config.getMinimumIdle());

        return new HikariDataSource(config);

    }
}