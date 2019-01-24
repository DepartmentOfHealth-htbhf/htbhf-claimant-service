update claimant set date_of_birth = '1900-01-01' where date_of_birth is null;

alter table claimant ALTER COLUMN date_of_birth SET not null;
