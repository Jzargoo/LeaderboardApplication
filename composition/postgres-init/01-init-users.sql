DO
$$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'jzargo') THEN
      CREATE ROLE jzargo LOGIN PASSWORD 'root333';
   END IF;
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'debezium') THEN
      CREATE ROLE debezium 
	REPLICATION
	LOGIN PASSWORD 'dbz123';
   END IF;
END
$$;
