create table claimant (
    id bigserial not null primary key,
    claimantId varchar(16) not null,
    claimant_name varchar(100) not null
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.claimant
    OWNER to postgres;
