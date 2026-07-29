#!/usr/bin/env bash
# Copies Third Ball application data from the local Docker PostgreSQL database
# to a fresh Supabase project whose schema has already been created by Flyway.
#
# Required environment variable:
#   SUPABASE_DB_URL - Supabase direct or session-pooler PostgreSQL URL.
#
# Usage:
#   export SUPABASE_DB_URL='postgresql://...'
#   bash scripts/copy-local-data-to-supabase.sh
#
# This script intentionally refuses to import into a target that already has
# Third Ball rows. It does not modify the local source database.

set -Eeuo pipefail

readonly script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly dump_file="$(mktemp "${TMPDIR:-/tmp}/thirdball-supabase-data.XXXXXX")"

cleanup() {
  rm -f "$dump_file"
}
trap cleanup EXIT

if [[ -z "${SUPABASE_DB_URL:-}" ]]; then
  echo "SUPABASE_DB_URL is required. Copy the direct or session-pooler URI from Supabase Connect." >&2
  exit 1
fi

cd "$script_directory/.."

shopt -s nullglob
migration_files=(src/main/resources/db/migration/V*__*.sql)
readonly expected_flyway_migrations="${#migration_files[@]}"

if ! docker compose ps --status running --services | grep -qx 'postgres'; then
  echo "The local PostgreSQL Docker service is not running. Start it with: docker compose up -d postgres" >&2
  exit 1
fi

target_flyway_count="$(
  docker compose exec -T -e SUPABASE_DB_URL postgres sh -ceu \
    'psql "$SUPABASE_DB_URL" -At -v ON_ERROR_STOP=1 -c "SELECT CASE WHEN to_regclass('"'"'public.flyway_schema_history'"'"') IS NULL THEN -1 ELSE (SELECT count(*) FROM public.flyway_schema_history WHERE success) END;"'
)"

if [[ "$target_flyway_count" != "$expected_flyway_migrations" ]]; then
  echo "Supabase does not have the expected successful Third Ball Flyway migrations." >&2
  echo "Point the Spring Boot app at Supabase once so Flyway can apply its schema, then run this script again." >&2
  exit 1
fi

target_row_count="$(
  docker compose exec -T -e SUPABASE_DB_URL postgres sh -ceu \
    'psql "$SUPABASE_DB_URL" -At -v ON_ERROR_STOP=1 -c "SELECT (SELECT count(*) FROM public.players) + (SELECT count(*) FROM public.tournaments) + (SELECT count(*) FROM public.matches) + (SELECT count(*) FROM public.practice_sessions);"'
)"

if [[ "$target_row_count" != "0" ]]; then
  echo "Supabase already contains $target_row_count Third Ball rows. Refusing to overwrite an existing target." >&2
  exit 1
fi

echo "Exporting local Third Ball application data..."
docker compose exec -T postgres pg_dump \
  -U thirdball \
  -d thirdball \
  --data-only \
  --format=custom \
  --schema=public \
  --exclude-table=public.flyway_schema_history \
  --no-owner \
  --no-privileges \
  > "$dump_file"

echo "Restoring data to Supabase..."
docker compose exec -T -e SUPABASE_DB_URL postgres sh -ceu \
  'pg_restore --dbname="$SUPABASE_DB_URL" --format=custom --data-only --no-owner --no-privileges --single-transaction --exit-on-error --verbose' \
  < "$dump_file"

echo "Verifying row counts..."
local_counts="$(
  docker compose exec -T postgres psql -U thirdball -d thirdball -At -v ON_ERROR_STOP=1 -c \
    "SELECT count(*) FROM players UNION ALL SELECT count(*) FROM tournaments UNION ALL SELECT count(*) FROM matches UNION ALL SELECT count(*) FROM practice_sessions;"
)"
supabase_counts="$(
  docker compose exec -T -e SUPABASE_DB_URL postgres sh -ceu \
    'psql "$SUPABASE_DB_URL" -At -v ON_ERROR_STOP=1 -c "SELECT count(*) FROM public.players UNION ALL SELECT count(*) FROM public.tournaments UNION ALL SELECT count(*) FROM public.matches UNION ALL SELECT count(*) FROM public.practice_sessions;"'
)"

if [[ "$local_counts" != "$supabase_counts" ]]; then
  echo "Migration completed, but source and Supabase row counts differ. Do not switch the application yet." >&2
  exit 1
fi

docker compose exec -T -e SUPABASE_DB_URL postgres sh -ceu \
  'psql "$SUPABASE_DB_URL" -v ON_ERROR_STOP=1 -c "VACUUM ANALYZE;"'

echo "Supabase migration complete. Source and target row counts match."
