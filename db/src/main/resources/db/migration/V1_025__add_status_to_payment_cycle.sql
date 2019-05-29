alter table payment_cycle add column payment_cycle_status varchar(50);
-- This status is safe at this time because partial payments (due to balance) cause this and this story implements them (so none made yet)
update payment_cycle set payment_cycle_status = 'FULL_PAYMENT_MADE' where payment_cycle_status is null;
alter table payment_cycle alter column payment_cycle_status set not null;
