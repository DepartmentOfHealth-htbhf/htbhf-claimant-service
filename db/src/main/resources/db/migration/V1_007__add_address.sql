create table address (
    id uuid not null primary key,
    address_line_1 varchar(500) not null,
    address_line_2 varchar(500),
    town_or_city varchar(500) not null,
    postcode varchar(7) not null
);

alter table claimant add column card_delivery_address_id uuid;
alter table claimant add constraint fk_address foreign key (card_delivery_address_id) references address(id);
