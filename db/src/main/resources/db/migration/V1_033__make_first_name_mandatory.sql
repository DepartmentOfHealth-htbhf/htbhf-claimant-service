update claimant set first_name = '' where first_name is null;
alter table claimant alter column first_name set not null;
