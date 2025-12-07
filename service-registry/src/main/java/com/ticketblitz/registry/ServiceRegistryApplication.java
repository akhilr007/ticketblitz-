package com.ticketblitz.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
/**
 * Service Registry - Netflix Eureka Server
 *
 * WHAT THIS DOES :
 * =================
 * - Provides a central registry for all microservices
 * - Services register themselves on startup
 * - Services discover each other through this registry
 * - Monitors service health with heartbeats
 * - Automatically removes dead services
 *
 * MICROSERVICE PATTERN :
 * ======================
 * Service Discovery Pattern (Client-Side Discovery)
 *
 * HOW IT WORKS :
 * ===============
 * 1. catalog-service starts -> Registers with Eureka
 * 2. booking-service starts -> Registers with Eureka
 * 3. api-gateway asks Eureka: "Where is booking-service"
 * 4. Eureka returns: ["http://192.168.1.10:8082", "http://192.168.1.11:8082"]
 * 5. api-gateway load balances between instances
 *
 * DASHBOARD :
 * =============
 * Access http://localhost:8761
 * Shows all registered services, health-status, instances
 *
 * WHY NETFLIX EUREKA :
 * =====================
 * - Battle tested at Netflix (millions of services)
 * - Self-healing (removes dead instances automatically)
 * - No single point of failure (can run multiple Eureka servers)
 * - Client side load balancing (no proxy bottleneck)
 * - Rich dashboard for monitoring
 *
 * ALTERNATIVES :
 * ==============
 * Consul(Hashicorp), Kubernetes Service Discoery etc
 *
 * FOR THIS PROJECT :
 * ===================
 * Eureka is perfect because :
 * - Simple setup (2 annotations)
 * - Spring cloud native
 * - Great for learning
 * - Shows understanding of service discovery
 *
 * @author Akhil Rajan
 */
public class ServiceRegistryApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceRegistryApplication.class, args);
    }
}