#!/usr/bin/env bash

echo "🧹 Cleaning up TicketBlitz Infrastructure..."

cd ../docker || exit 1

docker compose down -v

echo "✅ All containers and volumes removed!"