-- H2 database (used in test) does not allow multiple drop column commands in single statement.
-- therefore split each drop column into it's own statement
alter table claim drop column if exists next_payment_date;
alter table claim drop column if exists voucher_entitlement;
