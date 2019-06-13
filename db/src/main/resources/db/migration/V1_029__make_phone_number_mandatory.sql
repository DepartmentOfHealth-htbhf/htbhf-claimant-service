update claimant set phone_number = '+44000000000' where phone_number is null;
alter table claimant alter column phone_number set not null;
