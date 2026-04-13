# 🛠️ TicketBlitz Setup Guide

Follow these instructions to run TicketBlitz locally for development and testing.

## Prerequisites
- Java 21+
- Maven 3.8+
- Docker Engine & Docker Compose
- - K6 (for load testing)
- Port 8080, 8761, 5432, 6379, 5672 available

## 1. Clone the Repository
```bash
git clone https://github.com/akhilr007/ticketblitz-.git
cd ticketblitz
```

### 2. Set Environment Variables
Create a local `.env` file or export these variables in your terminal:
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

## 4. Starting the Microservices
It is heavily recommended to start `service-registry` first, wait for it to boot on port 8761, and then start the rest.

*Open separate terminal tabs for each service:*
```bash
# Terminal 1 - Registry
cd service-registry && mvn spring-boot:run

# Terminal 2 - Gateway
cd api-gateway && mvn spring-boot:run

# Terminal 3 - Catalog
cd catalog-service && mvn spring-boot:run

# Terminal 4 - Booking
cd booking-service && mvn spring-boot:run

# Terminal 5 - Fulfillment
cd fulfillment-service && mvn spring-boot:run
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