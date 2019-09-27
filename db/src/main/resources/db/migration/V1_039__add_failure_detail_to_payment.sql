alter table payment add column failure_detail varchar;
alter table payment alter column payment_amount_in_pence drop not null;