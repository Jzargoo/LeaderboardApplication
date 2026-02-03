#!/bin/bash
set -euo pipefail

USER="${POSTGRES_USER:-${POSTGRESQL_USERNAME:-postgres}}"
DB="${POSTGRES_DB:-${POSTGRESQL_DATABASE:-postgres}}"
PASSWORD="${POSTGRES_PASSWORD:-${POSTGRESQL_PASSWORD:-}}"

if [ -z "$PASSWORD" ]; then
  echo "WARN: no password provided in POSTGRES_PASSWORD or POSTGRESQL_PASSWORD — psql may prompt for password"
else
  export PGPASSWORD="$PASSWORD"
fi

psql -v ON_ERROR_STOP=1 --username "$USER" --dbname "$DB" <<'EOSQL'
ALTER SYSTEM SET wal_level TO logical;
EOSQL

