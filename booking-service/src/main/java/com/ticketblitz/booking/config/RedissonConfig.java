package com.ticketblitz.booking.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson configuration - Distributed locking
 *
 * Why Redisson ?
 * Redisson provides production grade distributed locks:
 *  - automatic lock renewal (watchdog mechanism)
 *  - fair locks (FIFO ordering)
 *  - try-lock with timeout
 *  - automatic unlock on connection loss
 *
 * Alternatives rejected:
 * - Spring Data redis: No distributed locks
 * - Jedis: Redis client, no lock abstractions
 * - Database locks only: doesn't work across instance
 *
 * Distributed lock pattern
 * -------------------------
 * When multiple service instances compete  for same resource:
 *
 * Instance 1: lock("seat:123") -> SUCCESS -> process locking
 * Instance 2: lock("seat:123") -> WAIT -> wait for lock
 *
 * Only one instance can hold lock at a time
 * Others wait in a queue (fair lock) or timeout
 *
 *
 * Lock keys
 * ----------
 * - "booking:seat:{eventId}:{seatId}" - Individual seat lock
 * - "booking:event:{eventId}" - event-level lock (for bulk ops)
 * - "booking:user:{userId}" - user level lock (rate limiting)
 *
 *
 * Watchdog mechanism
 * -------------------
 * Redisson automatically renews locks every 10 seconds
 * If service crashes, lock auto-release after 30 seconds
 * Prevents deadlock from crashed instances
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private String redisPort;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setConnectionPoolSize(64)  // max connections
                .setConnectionMinimumIdleSize(16) // min idle connections
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500) // retry interval
                .setPingConnectionInterval(30000); // keep-alive ping

        // watchdog timeout (lock auto-renewal)
        config.setLockWatchdogTimeout(30000); // 30 seconds

        return Redisson.create(config);
    }
}