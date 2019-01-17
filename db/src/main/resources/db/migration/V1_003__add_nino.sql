alter table claimant ADD COLUMN nino varchar(9);

update claimant set nino = 'QQ123456C' where nino is null;

alter table claimant ALTER COLUMN nino SET not null;
