update claimant set address_id = card_delivery_address_id where address_id is null;
alter table claimant drop column card_delivery_address_id;
alter table claimant alter column address_id set not null;