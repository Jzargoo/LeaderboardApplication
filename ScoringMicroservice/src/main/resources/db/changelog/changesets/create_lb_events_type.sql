CREATE TABLE IF NOT EXISTS lb_events_type (
    id SERIAL PRIMARY KEY,
    event_name VARCHAR(255) NOT NULL,
    event_score DECIMAL(10, 2) NOT NULL,
    description VARCHAR(512),
    UNIQUE (event_score, event_name)
);