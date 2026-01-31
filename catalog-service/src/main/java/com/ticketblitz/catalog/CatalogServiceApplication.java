package com.ticketblitz.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Catalog Service Application
 *
 * PURPOSE:
 * ========
 * Read-heavy microservice for event browsing and venue management
 *
 * FEATURES:
 * ===========
 * - Event listing with search/filter
 * - Event details with seat availability
 * - Venue management
 * - Redis caching for performance
 * - Public endpoints (no authentication required)
 *
 * ARCHITECTURE:
 * =============
 * - Registers with Eureka for service discovery
 * - Accessible via API GATEWAY at /api/events/**
 * - Uses PostgreSQL for persistence
 * - Uses Redis for caching hot data
 *
 * CACHING STRATEGY:
 * =================
 * - Event lists: 5 minutes TTL
 * - Event details: 10 minutes TTL
 * - Seat availability: 30 seconds TTL (frequently updated)
 * - Venue data: 1 hour TTL (rarely changes)
 *
 * @Akhil
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
public class CatalogServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}