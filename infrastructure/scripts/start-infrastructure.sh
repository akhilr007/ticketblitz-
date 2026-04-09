#!/usr/bin/env bash

echo "🚀 Starting TicketBlitz Infrastructure..."

cd ../docker || exit 1

# Start infrastructure + observability containers
# Application services should be run locally with: mvn spring-boot:run
docker compose up -d postgres redis rabbitmq prometheus grafana tempo loki promtail

echo ""
echo "⏳ Waiting for services to be healthy..."

while true; do
  STATUS=$(docker compose ps --format json | grep -c '"Health":"starting"')
  if [ "$STATUS" -eq 0 ]; then
    break
  fi
  sleep 2
done

echo ""
echo "✅ Infrastructure status:"
docker compose ps

echo ""
echo "📊 Access Points:"
echo "  Redis:       localhost:6379"
echo "  PostgreSQL:  localhost:5432"
echo "  RabbitMQ:    localhost:5672 (AMQP)"
echo "  RabbitMQ UI: http://localhost:15672 (ticketblitz / ticketblitz-password)"
echo ""
echo "🔭 Observability:"
echo "  Grafana:     http://localhost:3000 (admin/admin)"
echo "  Prometheus:  http://localhost:9090"
echo "  Tempo:       http://localhost:3200 (Distributed Tracing)"
echo "  Loki:        http://localhost:3100 (Log Aggregation)"
echo ""
echo "✅ Infrastructure ready!"