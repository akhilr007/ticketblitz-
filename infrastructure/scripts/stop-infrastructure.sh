#!/usr/bin/env bash

echo "🛑 Stopping TicketBlitz Infrastructure..."

cd ../docker || exit 1

docker compose down -v

echo "✅ Infrastructure stopped!"