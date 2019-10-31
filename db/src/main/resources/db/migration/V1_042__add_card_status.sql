alter table claim add column card_status varchar(50);
update claim set card_status = 'ACTIVE' where claim.claim_status = 'ACTIVE' or claim.claim_status = 'NEW';
update claim set card_status = 'PENDING_CANCELLATION' where claim.claim_status = 'PENDING_EXPIRY' or claim.claim_status = 'EXPIRED';

alter table claim add column card_status_timestamp timestamp;
update claim set card_status_timestamp = claim_status_timestamp where claim.card_status is not null;
