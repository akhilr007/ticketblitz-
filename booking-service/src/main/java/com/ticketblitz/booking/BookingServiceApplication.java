package com.ticketblitz.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Booking Service Application - High concurrency (core)
 *
 * Purpose:
 * ==========
 * - Handles seat reservation, payments, and booking confirmations
 * - This is the most CRITICAL service - handles money and inventory
 *
 * Concurrency Strategy:
 * =====================
 * 1. Pessimistic Locking (SELECT FOR UPDATE)
 *  - Prevents double-booking at database level
 *  - Used for seat selection
 *
 * 2. Redis distributed lock (Redisson)
 *  - Coordinates b/w multiple service instances
 *  - Prevents race conditions in distributed environment
 *
 * 3. Optimistic locking (@Version)
 *  - Fallback mechanism for updates
 *  - Detects concurrent modifications
 *
 * 4. Idempotency keys
 *  - Prevents duplicate bookings from retries
 *  - Critical for payment processing
 *
 * Booking Flow:
 * ================
 * 1. Reserve Seats (Pending)
 *  - Lock seats with SELECT FOR UPDATE
 *  - Create booking in pending state
 *  - Set 10 minute timeout
 *
 * 2. Process Payment (Reserved -> Confirmed)
 *  - Mock payment gateway call
 *  - Update booking status
 *  - Release locks
 *
 * 3. Confirm booking (Confirmed)
 *  - Publish event to RabbitMQ
 *  - Fulfillment service generates tickets
 *
 * 4. Timeout Handling
 *  - Scheduled job checks for expired reservations
 *  - Automatically cancels and releases state
 *
 *  Features:
 *  ===========
 *  - Reserve seats with distributed locking
 *  - Process payment (mock gateway)
 *  - Confirm bookings
 *  - Cancel bookings
 *  - View user's booking history
 *  - Automatic timeout handling
 *
 *  Integrations:
 *  ==============
 *  - Catalog service: Check seat availability (feign client)
 *  - Payment gateway: Process payments (mock)
 *  - Fulfillment service: Ticket generation (RabbitMQ)
 *
 * @author Akhil
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
@EnableScheduling
public class BookingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}