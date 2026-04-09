# 🛠️ TicketBlitz Setup Guide

Follow these instructions to run TicketBlitz locally for development and testing.

## Prerequisites
- Java 17+
- Maven 3.6+
- Docker Engine & Docker Compose
- Port 8080, 8761, 5432, 6379, 5672 available

## 1. Environment Variables
Create a local `.env` file or export these variables in your terminal:
```bash
export JWT_SECRET="your_32_character_ultra_secure_super_secret"
export POSTGRES_PASSWORD="ticketblitz_password"
```

## 2. Bootstrapping Infrastructure
Start the backing services (Databases, Cache, Message Brokers) using Docker.
```bash
docker-compose -f infrastructure/docker/docker-compose.yml up -d
```
Verify they are running:
```bash
docker ps
```

## 3. Starting the Microservices
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

## 4. Verification
1. Open the [Eureka Dashboard](http://localhost:8761) and verify 4 instances are registered (`API-GATEWAY`, `CATALOG-SERVICE`, `BOOKING-SERVICE`, `FULFILLMENT-SERVICE`).
2. Hit the [Swagger UI](http://localhost:8080/swagger-ui.html) to interact with the API endpoints.
