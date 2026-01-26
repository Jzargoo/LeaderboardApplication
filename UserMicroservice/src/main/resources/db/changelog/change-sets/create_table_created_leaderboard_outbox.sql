CREATE TABLE public.created_leaderboard_outbox
(
    id varchar(128) PRIMARY KEY,
    user_id bigint REFERENCES users,
    lb_id varchar(256),
    saga_id varchar(256)
);