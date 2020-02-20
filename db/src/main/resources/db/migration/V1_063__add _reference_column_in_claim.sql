alter table claim add column reference varchar(10);
update claim  set reference = upper(substring(replace(id::varchar, '-',''), 1, 10)) where reference is null;
alter table claim alter column reference SET not null;
create unique index unique_claim_reference on claim (reference);