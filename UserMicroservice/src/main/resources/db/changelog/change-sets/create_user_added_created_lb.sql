CREATE TABLE user_added_created_lb
(
    id varchar(128) PRIMARY KEY,
    saga_id varchar(128),
    user_id bigint REFERENCES users,
    lb_id varchar(128)
);