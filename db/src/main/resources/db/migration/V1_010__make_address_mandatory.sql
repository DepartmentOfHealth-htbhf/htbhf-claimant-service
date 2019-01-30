delete from claimant where card_delivery_address_id is null;
alter table claimant alter column card_delivery_address_id set not null;
