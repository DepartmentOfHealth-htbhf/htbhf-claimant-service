drop index unique_nino_for_active_claim;

alter table claim alter column email_address drop not null;
alter table claim alter column phone_number drop not null;
alter table claim alter column nino drop not null;

create unique index unique_nino_for_active_claim on claim (nino)
where claim_status in ('NEW', 'ACTIVE', 'PENDING', 'PENDING_EXPIRY') and nino is not null;