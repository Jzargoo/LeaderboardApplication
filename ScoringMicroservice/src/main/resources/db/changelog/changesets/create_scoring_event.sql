CREATE TABLE scoring_event (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_name VARCHAR(255) NOT NULL,
    event_score DECIMAL(10, 2) NOT NULL,
    UNIQUE (event_name,event_score)
);