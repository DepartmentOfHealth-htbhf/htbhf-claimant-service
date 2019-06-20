alter table claimant add column email_address varchar(256);
update claimant set email_address = '' where email_address is null;
alter table claimant alter column email_address set not null;
