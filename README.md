# 🎟️ TicketBlitz - High-Concurrency Event Ticketing Platform

> Enterprise-grade microservices demonstrating distributed systems patterns, service discovery, API gateway, and async processing.

## 🏗️ Architecture
```
User → API Gateway → Service Registry (Eureka)
                           ↓
        ┌──────────────────┼──────────────────┐
        ↓                  ↓                  ↓
  Catalog Service    Booking Service    Fulfillment Service
        ↓                  ↓                  ↓
        └──────────────────┼──────────────────┘
                           ↓
              PostgreSQL + Redis + RabbitMQ
```

## 🚀 Tech Stack

- **Java 17** + **Spring Boot 3.2**
- **Spring Cloud** (Eureka, Gateway, LoadBalancer)
- **PostgreSQL 15** (Primary database)
- **Redis 7** (Cache + Distributed locks)
- **RabbitMQ** (Message broker)
- **Prometheus + Grafana** (Observability)
- **Docker Compose** (Local deployment)

## 📦 Microservices

| Service | Port | Responsibility |
|---------|------|----------------|
| **Service Registry** | 8761 | Service discovery (Eureka) |
| **API Gateway** | 8080 | Auth, routing, rate limiting |
| **Catalog Service** | 8081 | Event browsing (read-heavy) |
| **Booking Service** | 8082 | Seat reservations (write-heavy) |
| **Fulfillment Service** | 8083 | Ticket generation (async worker) |

## 🎯 Microservice Patterns Implemented

- ✅ **Service Discovery** (Eureka)
- ✅ **API Gateway** (Spring Cloud Gateway)
- ✅ **Circuit Breaker** (Resilience4j)
- ✅ **Distributed Data Consistency** (Synchronous/Async Compensation)
- ✅ **Event-Driven Architecture** (RabbitMQ)
- ✅ **Service-Level Read/Write Separation** (Catalog reads, Booking writes)
- ✅ **Database per Service**
- ✅ **Distributed Locking** (Redis)
- ✅ **Bulkhead Pattern** (Thread pool isolation)
- ✅ **Rate Limiting** (API Gateway)

## 🛠️ Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker & Docker Compose

### Environment Variables
Before running the services, ensure you have set the following environment variables:
```bash
export JWT_SECRET=your_super_secret_jwt_key_at_least_32_chars
export POSTGRES_PASSWORD=ticketblitz_password
```

### Build & Run
```bash
# Build all services
mvn clean install

# Start infrastructure (PostgreSQL, Redis, RabbitMQ)
docker-compose -f infrastructure/docker/docker-compose.yml up -d

# Start services (in separate terminals)
cd service-registry && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
cd catalog-service && mvn spring-boot:run
cd booking-service && mvn spring-boot:run
cd fulfillment-service && mvn spring-boot:run
```

### Access Points

- **Eureka Dashboard**: http://localhost:8761
- **API Gateway**: http://localhost:8080
- **API Docs (Swagger)**: http://localhost:8080/swagger-ui.html
- **Grafana**: http://localhost:3000

### Sample API Requests
```bash
# 1. View active events via Catalog Service
curl -X GET http://localhost:8080/api/v1/events

# 2. Book a ticket (Requires JWT Auth via API Gateway)
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": 1,
    "seatIds": [101, 102],
    "idempotencyKey": "uuid-1234-5678"
  }'
```

### Troubleshooting
- **Connection Refused (Postgres/Redis/RabbitMQ)**: Ensure Docker containers are fully up (`docker ps`).
- **Services de-registering from Eureka**: Ensure you allocate enough RAM to Docker, or simply restart the affected service.
- **Unauthorized (401)**: Did you pass the `Bearer` token in the header and set `JWT_SECRET`?

## 📊 Key Features

### 1. Distributed Seat Locking
- Redis-based TTL locks (10-minute hold)
- Automatic expiry and cleanup
- Zero double-bookings under load

### 2. Optimistic Concurrency Control
- Version-based locking in PostgreSQL
- Handles 1000+ simultaneous booking attempts
- Graceful conflict resolution

### 3. Asynchronous Ticket Generation
- RabbitMQ-based message processing
- Decoupled PDF generation and email delivery
- <200ms API response times

### 4. Circuit Breaker Pattern
- Resilience4j integration
- Automatic failover for external services
- Fallback mechanisms for payment gateway

## 🧪 Testing
```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# Load testing (requires k6)
k6 run infrastructure/load-tests/booking-storm.js
```

## 📈 Performance Metrics

- **Throughput**: 450+ bookings/second
- **P99 Latency**: <300ms
- **Concurrency**: 1000+ simultaneous users
- **Success Rate**: 99.9% under peak load

## 📚 Documentation

- [Architecture Diagram](docs/architecture.md)
- [Setup Guide](docs/setup.md)
- [API Documentation](docs/api.md)
- [Microservice Patterns](docs/patterns.md)

## 🎓 Learning Outcomes

This project demonstrates:
- Microservices architecture with Spring Cloud
- Service discovery and dynamic routing
- Distributed transactions (Saga pattern)
- High-concurrency handling
- Message-driven architecture
- Production-grade observability
- Load testing and performance optimization

## 📝 License

MIT License - Free for learning and portfolio use

---

**Built for SDE 2 interview preparation** 🎯
