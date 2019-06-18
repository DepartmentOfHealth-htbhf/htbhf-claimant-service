alter table message_queue add column created_timestamp timestamp default now();
update message_queue set created_timestamp = message_timestamp where created_timestamp is null;
