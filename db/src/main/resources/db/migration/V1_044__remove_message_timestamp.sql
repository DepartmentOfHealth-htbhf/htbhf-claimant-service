update message_queue set process_after = message_timestamp where process_after is null;
alter table message_queue drop column message_timestamp;
