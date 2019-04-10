alter table claimant add column claim_status varchar(20);

update claimant set claim_status = 'ACTIVE' where claim_status is null and eligibility_status = 'ELIGIBLE';

update claimant set claim_status = 'REJECTED' where claim_status is null;

alter table claimant alter column claim_status set not null;

create index claimant_claim_status_idx on claimant (claim_status);
