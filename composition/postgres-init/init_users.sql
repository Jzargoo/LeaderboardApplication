DO
$body$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE rolname = 'postgres'
   ) THEN
      CREATE ROLE postgres WITH SUPERUSER LOGIN PASSWORD 'root333';
   END IF;
END
$body$;

DO
$body$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE rolname = 'jzargo'
   ) THEN
      CREATE ROLE jzargo WITH LOGIN PASSWORD 'root333' CREATEROLE CREATEDB;
   END IF;
END
$body$;

DO
$body$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE rolname = 'debezium'
   ) THEN
      CREATE ROLE debezium WITH LOGIN PASSWORD 'dbz123' REPLICATION;
   END IF;
END
$body$;

GRANT ALL PRIVILEGES ON DATABASE generaldb TO jzargo;
GRANT USAGE ON

