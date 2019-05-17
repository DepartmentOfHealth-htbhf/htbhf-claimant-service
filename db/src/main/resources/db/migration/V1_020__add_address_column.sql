alter table claimant add column address_id uuid;
update claimant set address_id = card_delivery_address_id where address_id is null;
alter table claimant alter column card_delivery_address_id drop not null;