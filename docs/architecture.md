# 🏗️ TicketBlitz Architecture

## High-Level Data Flow

TicketBlitz is designed to handle extremely high-velocity, high-contention traffic (e.g., ticket releases).

```text
User ──> API Gateway (8080) ──> [Eureka Service Registry]
              │
              ├──> Catalog Service (Reads/Event browsing)
              │      └──> PostgreSQL / Redis / Elasticsearch
              │
              ├──> Booking Service (Writes/Checkout)
              │      ├──> PostgreSQL (Transactional bookings)
              │      ├──> Redis (Distributed Row-level locks)
              │      └──> RabbitMQ (Event Producer)
              │
              └──> Fulfillment Service (Async PDF Generation)
                     ├──> RabbitMQ (Event Consumer)
                     └──> PostgreSQL (Ticket generation records)
```

## Core Components
1. **API Gateway (Spring Cloud Gateway):** Acts as the single entry point. Enforces JWT validation, CORS, Rate Limiting (via Redis), and routes traffic to downstream services.
2. **Catalog Service:** Handles read-heavy traffic. Browses events, checks venue availability.
3. **Booking Service:** Handles write-heavy checkout flows. Integrates Distributed Redis Locks to prevent overselling of seats, and implements local transactional logic.
4. **Fulfillment Service:** Runs in the background picking up `BookingConfirmedEvent` messages from RabbitMQ to asynchronously generate PDFs and dispatch emails.

## Distributed Data Consistency
To avoid complex distributed two-phase commits (2PC), the system utilizes:
1. **Local Transactions:** Booking data is saved locally to the Postgres instance attached to `booking-service`.
2. **Synchronous Seat Locks:** Booking verifies availability across the Catalog synchronously.
3. **Eventual Consistency:** Fully confirmed bookings are dispatched asynchronously to fulfillment.
