alter table payment
    add column request_reference  varchar not null default '',
    add column response_reference varchar,
    drop column payment_reference;
