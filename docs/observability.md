# 🔭 Observability Guide

TicketBlitz uses the **Grafana Observability Stack** (Prometheus + Tempo + Loki) for production-grade monitoring across all microservices.

## Architecture

```mermaid
flowchart LR

%% ========== USERS ==========
U[User / Client]

%% ========== EDGE ==========
subgraph EDGE["Edge Layer"]
    GW[API Gateway :8080]
end

%% ========== SERVICES ==========
subgraph SERVICES["Microservices Layer"]
    CS[Catalog Service :8081]
    BS[Booking Service :8082]
    FS[Fulfillment Service :8083]
end

%% ========== OBSERVABILITY ==========
subgraph OBS["Observability Platform"]
    P[Prometheus<br/>(Metrics)]
    T[Tempo<br/>(Tracing)]
    L[Loki<br/>(Logs)]
    G[Grafana<br/>(Dashboard)]
end

%% ========== FLOW: REQUEST ==========
U --> GW
GW --> CS
GW --> BS
GW --> FS

%% ========== METRICS ==========
GW --> P
CS --> P
BS --> P
FS --> P

%% ========== TRACES ==========
GW --> T
CS --> T
BS --> T
FS --> T

%% ========== LOGS ==========
CS --> L
BS --> L
FS --> L
GW --> L

%% ========== VISUALIZATION ==========
P --> G
T --> G
L --> G
```

## Three Pillars

### 1. Metrics (Prometheus + Micrometer)
- **JVM metrics**: heap, threads, GC, class loading
- **HTTP metrics**: request rate, error rate, latency percentiles (RED)
- **Custom business metrics**: bookings, payments, tickets, auth events
- **Infrastructure metrics**: HikariCP pool, Resilience4j circuit breakers

### 2. Traces (Tempo + Micrometer Tracing)
- End-to-end distributed tracing via OpenTelemetry → Zipkin format
- Automatic trace propagation across HTTP (Feign) and RabbitMQ
- `traceId` and `spanId` injected into every log line via MDC

### 3. Logs (Loki + Promtail + Logback)
- **Dev profile**: Human-readable console with `[traceId/spanId]`
- **Docker profile**: JSON structured logs via `logstash-logback-encoder`
- Promtail ships container logs to Loki with service labels

## Access Points

| Tool | URL | Credentials |
|------|-----|-------------|
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | — |
| Tempo | http://localhost:3200 | — |
| Loki | http://localhost:3100 | — |

## Pre-built Dashboards

### JVM & Service Health
Panels: service up/down, HTTP request/error rate, p95 latency, JVM heap/threads, HikariCP connections, GC pauses, circuit breaker state.

### Business Metrics
Panels: bookings created/cancelled rate, seats locked, payment success/failure ratio, payment p95 duration, tickets generated rate/duration/errors, gateway auth success/failure by reason, rate-limit rejections.

## Custom Metrics Reference

| Metric | Type | Service | Description |
|--------|------|---------|-------------|
| `ticketblitz.bookings.created` | Counter | booking | Bookings created |
| `ticketblitz.bookings.cancelled` | Counter | booking | Bookings cancelled |
| `ticketblitz.payments.processed` | Counter | booking | Payments (tag: status=success\|failed) |
| `ticketblitz.payments.duration` | Timer | booking | Payment processing latency |
| `ticketblitz.seats.locked` | Counter | booking | Seats locked for bookings |
| `ticketblitz.tickets.generated` | Counter | fulfillment | Tickets generated |
| `ticketblitz.tickets.generation.duration` | Timer | fulfillment | Ticket generation latency |
| `ticketblitz.tickets.generation.errors` | Counter | fulfillment | Ticket generation errors |
| `ticketblitz.tickets.duplicates.skipped` | Counter | fulfillment | Idempotent event skips |
| `ticketblitz.gateway.auth` | Counter | gateway | Auth events (tags: result, reason) |
| `ticketblitz.gateway.ratelimit.rejected` | Counter | gateway | Rate-limited requests |

## Tracing a Request End-to-End

1. Open Grafana → **Explore** → Select **Tempo** datasource
2. Search by service name or time range
3. Click a trace to see the full span waterfall
4. Click **Logs for this span** to jump to Loki logs filtered by traceId
5. In Loki, click the traceId link to jump back to Tempo

## Configuration

Tracing sampling rate is controlled per service in `application.yml`:
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% in dev, reduce to 0.1 in production
```