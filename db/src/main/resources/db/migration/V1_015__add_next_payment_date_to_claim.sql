alter table claim add column next_payment_date TIMESTAMP;

create index claim_next_payment_date_idx on claim(next_payment_date);
