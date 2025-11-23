CREATE TABLE
    created_leaderboards
(
    user_id          BIGINT REFERENCES users,
    id               BIGINT NOT NULL,
    leaderboard_name VARCHAR(255),
    PRIMARY KEY (user_id, id)
);
