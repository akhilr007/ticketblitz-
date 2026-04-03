package com.ticketblitz.fulfillment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Fulfillment Service — Async Ticket Generation Worker
 *
 * PURPOSE:
 * ========
 * Consumes BookingConfirmedEvent from RabbitMQ and generates
 * e-tickets for each seat in the confirmed booking.
 *
 * ARCHITECTURE:
 * =============
 * This service is an EVENT-DRIVEN WORKER, not a primary request handler.
 * It exposes a REST API only for ticket retrieval (read-heavy, simple queries).
 *
 * EVENT FLOW:
 * ===========
 * 1. BookingService confirms booking → publishes BookingConfirmedEvent
 * 2. This service consumes the event from RabbitMQ
 * 3. Generates one Ticket per seat (UUID ticket numbers, QR codes)
 * 4. Persists tickets to its own PostgreSQL database
 * 5. (Future) Sends email notifications with ticket attachments
 *
 * PATTERNS:
 * =========
 * - Event-Driven Architecture (choreography-based saga)
 * - Database per Service (own PostgreSQL schema)
 * - Idempotent Consumer (prevents duplicate tickets on redelivery)
 * - Dead-Letter Queue (poison messages routed to DLQ)
 *
 * INTEGRATIONS:
 * =============
 * - RabbitMQ: Consumes BookingConfirmedEvent
 * - PostgreSQL: Stores tickets
 * - Eureka: Service discovery registration
 * - API Gateway: Routes /api/tickets/** to this service
 *
 * @author Akhil
 */
@SpringBootApplication
@EnableDiscoveryClient
public class FulfillmentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FulfillmentServiceApplication.class, args);
    }
}
