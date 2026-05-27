#!/bin/bash
echo "Seeding development database..."
PGPASSWORD=dev123 psql -h localhost -U dev -d room_booking -f docs/db/seed.sql
echo "Seed complete!"
