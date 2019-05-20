alter table message_queue add column delivery_count int default 0;
alter table message_queue add column status varchar(20);
