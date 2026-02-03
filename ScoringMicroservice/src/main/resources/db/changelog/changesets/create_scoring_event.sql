CREATE TABLE IF NOT EXISTS scoring_events (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_name VARCHAR(255) NOT NULL,
    event_score DECIMAL(10, 2) NOT NULL
);