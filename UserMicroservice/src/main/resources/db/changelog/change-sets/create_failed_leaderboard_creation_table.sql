CREATE TABLE failed_leaderboard_creation(
    id SERIAL PRIMARY KEY,
    reason varchar(256),
    leaderboard_id varchar(128),
    user_id BIGINT REFERENCES users,
    saga_id varchar(128)
);