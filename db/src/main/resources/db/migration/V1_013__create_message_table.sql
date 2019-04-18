CREATE TABLE message_queue(
    id uuid not null primary key,
    message_timestamp TIMESTAMP not null DEFAULT NOW(),
    message_type varchar(50) not null,
    message_payload text not null
);
CREATE INDEX message_queue_message_type_timestamp_idx ON message_queue (message_type,message_timestamp);
