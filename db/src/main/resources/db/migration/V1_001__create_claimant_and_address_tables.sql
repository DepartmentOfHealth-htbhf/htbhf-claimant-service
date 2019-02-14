create table claimant (
    id uuid not null primary key,
    first_name varchar(500),
    last_name varchar(500) not null,
    nino varchar(9) not null,
    date_of_birth date not null,
    expected_delivery_date date,
    card_delivery_address_id uuid not null,
    created_timestamp TIMESTAMP DEFAULT NOW()
);

CREATE INDEX claimant_nino_idx ON claimant (nino);

create table address (
    id uuid not null primary key,
    address_line_1 varchar(500) not null,
    address_line_2 varchar(500),
    town_or_city varchar(500) not null,
    postcode varchar(8) not null
);

alter table claimant add constraint fk_address foreign key (card_delivery_address_id) references address(id);
