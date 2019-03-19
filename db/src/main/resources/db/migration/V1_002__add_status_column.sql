alter table claimant ADD COLUMN eligibility_status varchar(20);

update claimant set eligibility_status = 'ELIGIBLE' where eligibility_status is null;

alter table claimant ALTER COLUMN eligibility_status SET not null;