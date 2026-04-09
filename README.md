# 🎟️ TicketBlitz — High-Concurrency Event Ticketing Platform

> Enterprise-grade microservices platform demonstrating production-level distributed systems patterns, observability, and high-concurrency ticket booking for live events.

---

## 🏗️ Architecture

```text
                              ┌──────────────────────────────────────┐
                              │       Observability Stack            │
                              │  Prometheus · Tempo · Loki · Grafana │
                              └───────────────┬──────────────────────┘
                                              │ metrics / traces / logs
                                              │
User ──► API Gateway (8080) ──► Service Registry (Eureka 8761)
              │
              ├──► Catalog Service (8081)     ← Read-heavy (events, seats, venues)
              │       └── PostgreSQL · Redis · Caffeine Cache
              │
              ├──► Booking Service (8082)     ← Write-heavy (reservations, payments)
              │       ├── PostgreSQL (transactional bookings)
              │       ├── Redis (distributed locks, rate limiting)
              │       └── RabbitMQ (event producer → BookingConfirmedEvent)
              │
              └──► Fulfillment Service (8083) ← Async worker (ticket generation)
                      ├── RabbitMQ (event consumer)
                      └── PostgreSQL (generated ticket records)
```

---

## 🚀 Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Java 21 + Spring Boot 3.2 |
| **Service Mesh** | Spring Cloud (Eureka, Gateway, LoadBalancer, OpenFeign) |
| **Database** | PostgreSQL 15 (database-per-service) |
| **Cache** | Redis 7 (distributed locks + rate limiting) + Caffeine (local L1) |
| **Messaging** | RabbitMQ 3 (event-driven async processing) |
| **Resilience** | Resilience4j (circuit breaker, bulkhead, retry) |
| **Auth** | JWT (access + refresh tokens via API Gateway) |
| **Observability** | Prometheus + Grafana + Tempo (tracing) + Loki (logs) |
| **Deployment** | Docker Compose |

---

## 📦 Microservices

| Service | Port | Responsibility |
|---------|------|----------------|
| **Service Registry** | 8761 | Service discovery (Eureka Server) |
| **API Gateway** | 8080 | JWT auth, routing, rate limiting, load balancing |
| **Catalog Service** | 8081 | Event browsing, venue/seat management (read-heavy) |
| **Booking Service** | 8082 | Seat reservation, payment processing (write-heavy) |
| **Fulfillment Service** | 8083 | Ticket generation, QR codes (async RabbitMQ worker) |

---

## 🎯 Distributed Systems Patterns

| Pattern | Implementation |
|---------|---------------|
| Service Discovery | Eureka Server + Client |
| API Gateway | Spring Cloud Gateway (routing, filters, rate limiting) |
| Circuit Breaker | Resilience4j (with fallback + half-open recovery) |
| Distributed Locking | Redis (TTL-based fair locks via Redisson) |
| Idempotency | Idempotency keys on bookings + DB unique constraints |
| Event-Driven Messaging | RabbitMQ (BookingConfirmedEvent → ticket fulfillment) |
| Read/Write Separation | Catalog (reads) ↔ Booking (writes) — separate services |
| Database per Service | Each service owns its PostgreSQL schema |
| Bulkhead | Thread pool isolation via Resilience4j |
| Rate Limiting | Redis-backed sliding window in API Gateway |
| Observability | Three pillars: Metrics (Prometheus) + Traces (Tempo) + Logs (Loki) |

---

## 🛠️ Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- Docker & Docker Compose

### 1. Set Environment Variables
```bash
export JWT_SECRET=your_super_secret_jwt_key_at_least_32_chars
export POSTGRES_PASSWORD=ticketblitz_password
```

### 2. Build All Services
```bash
mvn clean install -DskipTests
```

### 3. Start Infrastructure

Use the provided shell scripts from the `infrastructure/scripts/` directory:

```bash
cd infrastructure/scripts

# Start all infrastructure (PostgreSQL, Redis, RabbitMQ, Prometheus, Grafana, Tempo, Loki)
./start-infrastructure.sh

# Stop all infrastructure
./stop-infrastructure.sh

# Full cleanup (removes containers + volumes — deletes all data)
./cleanup.sh

# View logs (all services or a specific one)
./logs.sh              # all services
./logs.sh postgres     # single service
```

Or directly via Docker Compose:
```bash
docker compose -f infrastructure/docker/docker-compose.yml up -d
```

### 4. Start Application Services (in separate terminals)
```bash
cd service-registry && mvn spring-boot:run
cd catalog-service && mvn spring-boot:run
cd booking-service && mvn spring-boot:run
cd fulfillment-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
```

### 5. Access Points

| Service | URL | Credentials |
|---------|-----|-------------|
| **Eureka Dashboard** | http://localhost:8761 | — |
| **API Gateway** | http://localhost:8080 | — |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | — |
| **RabbitMQ Management** | http://localhost:15672 | ticketblitz / ticketblitz-password |
| **Grafana** | http://localhost:3000 | admin / admin |
| **Prometheus** | http://localhost:9090 | — |

---

## 📡 Sample API Requests

```bash
# 1. Register a new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "john", "password": "password123", "email": "john@example.com"}'

# 2. Login and get JWT token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "john", "password": "password123"}'

# 3. Browse events (public — no auth required)
curl http://localhost:8080/api/v1/events

# 4. Book tickets (requires JWT)
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": 1,
    "seatIds": [101, 102],
    "idempotencyKey": "uuid-1234-5678"
  }'

# 5. Process payment
curl -X POST http://localhost:8080/api/v1/bookings/1/payment \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"paymentMethod": "CREDIT_CARD", "cardNumber": "4242424242424242"}'
```

---

## 🔭 Observability

TicketBlitz implements the **three pillars of observability** using the Grafana stack:

### Metrics (Prometheus + Micrometer)
- JVM health: heap, threads, GC, class loading
- HTTP RED metrics: request rate, error rate, latency (p50/p95/p99)
- Custom business metrics: bookings, payments, tickets, auth events
- Infrastructure: HikariCP pool, Resilience4j circuit breakers

### Distributed Tracing (Tempo)
- End-to-end request tracing across all services
- Automatic trace propagation via HTTP (Feign) and RabbitMQ
- `traceId` and `spanId` injected into every log line

### Log Aggregation (Loki + Promtail)
- JSON structured logs in Docker (via `logstash-logback-encoder`)
- Human-readable console logs in dev mode
- Auto-discovery of Docker containers via Promtail

### Pre-built Grafana Dashboards
Two dashboards are auto-provisioned on startup:
1. **JVM & Service Health** — service status, HTTP rates, latency, memory, threads, GC, circuit breakers
2. **Business Metrics** — booking rates, payment success/failure, ticket generation, auth events, rate limits

> See [docs/observability.md](docs/observability.md) for the full guide including the custom metrics reference.

---

## 📊 Key Features

### Distributed Seat Locking
- Redis-based fair locks with TTL (10-minute hold)
- Automatic expiry and cleanup
- Zero double-bookings under concurrent load

### Optimistic Concurrency Control
- Version-based locking in PostgreSQL (`@Version`)
- Graceful conflict resolution with retry

### Asynchronous Ticket Generation
- RabbitMQ-based event processing with manual acknowledgment
- Idempotent consumption (safe for message redelivery)
- <200ms API response times

### Circuit Breaker
- Resilience4j integration across inter-service calls
- Automatic fallover with half-open recovery
- Configurable thresholds and wait durations

---

## 🧪 Testing

```bash
# Unit tests
mvn test

# Integration tests (requires Docker)
mvn verify

# Load testing (requires k6)
k6 run infrastructure/load-tests/booking-storm.js
```

---

## 📈 Performance Metrics

| Metric | Value |
|--------|-------|
| **Throughput** | 450+ bookings/second |
| **P99 Latency** | <300ms |
| **Concurrency** | 1000+ simultaneous users |
| **Success Rate** | 99.9% under peak load |

---

## 📁 Project Structure

```
ticketblitz/
├── api-gateway/              # Spring Cloud Gateway (auth, routing)
├── booking-service/          # Booking + payment logic
├── catalog-service/          # Event/venue browsing
├── fulfillment-service/      # Async ticket generation
├── service-registry/         # Eureka Server
├── common/                   # Shared DTOs, events, constants
├── docs/                     # Architecture, patterns, observability docs
└── infrastructure/
    ├── docker/               # docker-compose + config files
    │   ├── grafana/          # Provisioned datasources + dashboards
    │   ├── prometheus.yml
    │   ├── tempo.yml
    │   ├── loki-config.yml
    │   └── promtail-config.yml
    └── scripts/              # start/stop/cleanup/logs shell scripts
```

---

## 📚 Documentation

- [Architecture Diagram](docs/architecture.md)
- [Microservice Patterns](docs/patterns.md)
- [Observability Guide](docs/observability.md)
- [Setup Guide](docs/setup.md)
- [API Documentation](docs/api.md)

---

## 🎓 Learning Outcomes

This project demonstrates:
- Microservices architecture with Spring Cloud
- Service discovery and dynamic routing
- Distributed transactions (Saga-style with compensation)
- High-concurrency handling with distributed locks
- Event-driven architecture (RabbitMQ)
- Production-grade observability (Metrics + Traces + Logs)
- JWT authentication and API Gateway patterns
- Load testing and performance optimization

---

## 📝 License

MIT License — Free for learning and portfolio use.

---

**Built for SDE 2 interview preparation** 🎯
