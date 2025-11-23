CREATE TABLE processing_messages(
    id varchar(128) PRIMARY KEY ,
    processed_at timestamp,
    message_type varchar(16)
)