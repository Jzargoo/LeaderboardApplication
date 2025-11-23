CREATE TABLE active_leaderboards
(
    user_id          BIGINT REFERENCES users,
    leaderboard_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, leaderboard_name)
);
