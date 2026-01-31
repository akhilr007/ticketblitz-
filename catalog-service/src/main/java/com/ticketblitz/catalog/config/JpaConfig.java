package com.ticketblitz.catalog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA Configuration
 *
 * KEY OPTIMIZATIONS:
 * ==================
 * 1. Batch inserts (hibernate.jdbc.batch_size=20)
 * 2. Order inserts/updates (better batching)
 * 3. Statement caching (in HikariCP)
 * 4. Query hints for performance
 *
 * TRANSACTION STRATEGY:
 * =====================
 * - Read only transactions for queries (optimization)
 * - Optimistic locking for updates (better concurrency)
 * - @Transactional at service layer (not repository)
 *
 * @author Akhil
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.ticketblitz.catalog.repository")
@EnableTransactionManagement
@EnableJpaAuditing
public class JpaConfig {

    // configuration via application.yml
    // this class exists for documentation and future customization
}