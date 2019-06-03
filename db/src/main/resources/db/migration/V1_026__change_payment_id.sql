alter table payment drop constraint payment_pkey;
alter table payment add primary key(id);
-- payment_reference was part of the primary key so automatically was made not null
alter table payment alter column payment_reference drop not null;
