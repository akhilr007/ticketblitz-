#!/usr/bin/env bash

echo "🚀 Starting TicketBlitz Infrastructure..."

cd ../docker || exit 1

docker compose up -d

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
echo "  Prometheus:  http://localhost:9090"
echo "  Grafana:     http://localhost:3000 (admin/admin)"
echo ""
echo "✅ Infrastructure ready!"