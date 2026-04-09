# 🧩 Microservice Patterns Implemented

TicketBlitz leverages several enterprise-level design patterns to guarantee resilience, throughput, and fault tolerance.

### 1. Circuit Breaker (Resilience4j)
**Where it's used:** API Gateway & Feign Clients.
**Why:** To prevent cascading failures. If the `catalog-service` is struggling under load, the circuit breaker opens and immediately returns a configured fallback response instead of blocking Gateway threads waiting for long timeouts.

### 2. Distributed Locks
**Where it's used:** `BookingService` (Seat Locking logic).
**Why:** Standard database transactions lock locally. In a distributed environment with millions of requests hitting independent instances, we need a centralized authority (Redis via Redisson) to acquire a lock on `booking:seat:100:54` to ensure exactly *one* user can hold that seat in their cart.

### 3. Idempotency Keys
**Where it's used:** Booking APIs and RabbitMQ Listeners.
**Why:** Network failures cause retries. If a user clicks "Checkout" twice, the `idempotencyKey` ensures only one checkout actually executes. In the fulfillment listener, if RabbitMQ delivers the same message twice, the system skips it.

### 4. Service-Level Read/Write Separation
**Where it's used:** `CatalogService` (Reads), `BookingService` (Writes).
**Why:** Spiky workloads impact ticket reads vastly out of proportion to writes. By isolating the read paths from the write paths via service boundaries, database locks required for writes will never slow down users simply browsing the venue map.

### 5. Event-Driven Messaging
**Where it's used:** RabbitMQ between Booking and Fulfillment.
**Why:** Creating a PDF, generating a QR code, and emailing a user takes ~2-5 seconds. If the `BookingService` waited for this, throughput would plummet. Disconnecting it via RabbitMQ guarantees sub-200ms booking confirmation, while Fulfillment churns through the backlog independently.

### 6. Observability (Grafana Stack)
**Where it's used:** All services via Prometheus (metrics), Tempo (traces), Loki (logs), and Grafana (dashboards).
**Why:** In a distributed system, debugging an issue that spans Gateway → Booking → Fulfillment is impossible with local logs alone. The three pillars of observability — metrics, traces, and logs — are correlated via `traceId`, enabling one-click navigation from a Prometheus alert to the exact trace and log lines that caused it. See [docs/observability.md](./observability.md) for full details.
