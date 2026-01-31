package com.ticketblitz.catalog.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Multi level cache configuration
 *
 * CACHING STRATEGY:
 * =================
 * L1 (Caffeine - In-Memory):
 * - Ultra-fast (nanosecond access)
 * - Per JVM instance
 * - For hot data (frequently accessed)
 *
 * L2 (Redis - Distributed):
 * - Fast (millisecond access)
 * - Shared across instances
 * - For warm data (ocassionaly accessed)
 *
 * CACHE HIERARCHY:
 * ================
 * Request -> L1 (caffeine) -> L2 (redis) -> database
 *           ↓ hit (ns)      ↓ hit (ms)    ↓ miss (100ms+)
 *
 * WHY TWO LEVELS:
 * - Reduce redis calls (saves network I/O)
 * - Better latency (L1 is 100x faster)
 * - Scale horizontally (L2 shared state)
 *
 * CACHE EVICTION POLICY:
 * - LRU (Least Recently used)
 * - Size based eviction
 * - TTL-based expiration
 *
 * @author Akhil
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    // cache names
    public static final String EVENT_LIST_CACHE = "events";
    public static final String EVENT_DETAILS_CACHE = "event_details";
    public static final String SEAT_AVAILABILITY_CACHE = "seat_availability";
    public static final String VENUE_CACHE = "venues";

    /**
     * L1 Cache - Caffeine (In-Memory)
     *
     * Used for ultra-hot-data that's accessed frequently
     *
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                EVENT_LIST_CACHE,
                EVENT_DETAILS_CACHE,
                VENUE_CACHE
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)     // max 100 entries per cache
                .expireAfterWrite(5, TimeUnit.MINUTES) // ttl
                .recordStats()); // enable metrics

        log.info("L1 cache (Caffeine) configured with maxSize=1000, TTL=5 min");

        return cacheManager;
    }

    /**
     * L2 cache - Redis (Distributed)
     *
     * Used for warm data that's shared across instances
     */
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        // Default configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()
                        )
                )
                .disableCachingNullValues();

        // per cache TTL configuration
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();


        // Event lists: 5 minutes (frequently updated)
        cacheConfigurations.put(EVENT_LIST_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Event details: 10 minutes (rarely updated)
        cacheConfigurations.put(EVENT_DETAILS_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Seat availability: 30 seconds (highly dynamic)
        cacheConfigurations.put(SEAT_AVAILABILITY_CACHE,
                defaultConfig.entryTtl(Duration.ofSeconds(30)));

        // Venue: 1 hour (static time)
        cacheConfigurations.put(VENUE_CACHE,
                defaultConfig.entryTtl(Duration.ofHours(1)));

        log.info("L2 Cache (Redis) configured with per-cache TTLs");

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

}