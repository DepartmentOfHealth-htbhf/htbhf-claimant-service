create table payment_cycle(
    id uuid not null primary key,
    version_number int not null,
    claim_id uuid not null references claim(id),
    card_account_id varchar(50) not null,
    cycle_start_date timestamp,
    cycle_end_date timestamp,
    eligibility_status varchar(50) not null,
    voucher_entitlement_json json not null,
    expected_delivery_date timestamp,
    children_dob_json json,
    total_vouchers int not null,
    total_entitlement_amount_in_pence int not null,
    card_balance_in_pence int default 0,
    card_balance_timestamp timestamp default now(),
    created_timestamp timestamp not null default now()
);

create index payment_cycle_cycle_end_date_idx on payment_cycle (cycle_end_date, eligibility_status);

create table payment(
    id uuid not null,
    claim_id uuid not null references claim(id),
    card_account_id varchar(50) not null,
    payment_amount_in_pence int not null,
    payment_timestamp timestamp not null,
    payment_reference varchar(50) not null,
    payment_status varchar(50) not null,
    created_timestamp timestamp not null default now(),
    payment_cycle_id uuid references payment_cycle(id),
    primary key (id, payment_reference)
);
