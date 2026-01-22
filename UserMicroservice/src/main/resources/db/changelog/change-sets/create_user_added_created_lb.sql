CREATE TABLE user_added_created_leaderboard
(
    id varchar(128) PRIMARY KEY,
    user_id bigint REFERENCES users,
    lb_id varchar(128)
);