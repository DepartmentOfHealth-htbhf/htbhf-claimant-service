alter table message_queue add column if not exists process_after timestamp;

update message_queue set process_after = message_timestamp where process_after is null;
