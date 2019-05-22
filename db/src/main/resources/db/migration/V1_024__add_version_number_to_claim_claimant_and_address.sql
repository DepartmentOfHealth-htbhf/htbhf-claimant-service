alter table claim add column version_number int;
update claim set version_number = 0 where version_number is null;
alter table claim alter column version_number set not null;

alter table claimant add column version_number int;
update claimant set version_number = 0 where version_number is null;
alter table claimant alter column version_number set not null;

alter table address add column version_number int;
update address set version_number = 0 where version_number is null;
alter table address alter column version_number set not null;
