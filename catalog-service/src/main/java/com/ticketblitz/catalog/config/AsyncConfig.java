package com.ticketblitz.catalog.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Processing Configuration
 *
 * CONCURRENCY PATTERN: Bulkhead Pattern
 * =====================================
 * Separate thread pools for different async operations
 * Prevents one slow operation from blocking others
 *
 * USE CASES:
 * - Cache warming (preload cache on startup)
 * - Event logging (dont block main thread)
 * - Analytics tracking (fire and forget)
 * - Email notifications (if we add them)
 *
 * THREAD POOL SIZING:
 * - Core pool:  Always alive threads
 * - Max pool: Max threads under load
 * - Queue: Buffer for burst traffic
 *
 * @author Akhil
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * General purpose async executor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5); // always alive
        executor.setMaxPoolSize(10); // peak capacity
        executor.setQueueCapacity(100); // buffer
        executor.setThreadNamePrefix("Async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("Async executor configured: core=5, max=10, queue=100");

        return executor;
    }

    /**
     * Cache operations executor (separate pool)
     */
    @Bean(name = "cacheExecutor")
    public Executor cacheExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("Cache-");

        executor.initialize();

        log.info("Cache executor configured: core=2, max=5, queue=50");

        return executor;
    }
}