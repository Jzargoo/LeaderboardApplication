CREATE TABLE
    IF NOT EXISTS
    categories
(
    id SERIAL PRIMARY KEY,
    name varchar(64),
    attributes jsonb
)